package com.howck.dmi.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import com.howck.dmi.DmiApp
import com.howck.dmi.MainActivity
import com.howck.dmi.R
import com.howck.dmi.model.MediaAction
import com.howck.dmi.model.MediaState
import com.howck.dmi.model.ShortcutId
import com.howck.dmi.ui.CapsuleOverlay
import com.howck.dmi.ui.dynamicColorSchemeFor

/**
 * Foreground service that keeps the floating capsule alive and owns its window.
 *
 * The window is a full-screen transparent [TYPE_APPLICATION_OVERLAY] surface
 * (FLAG_NOT_TOUCH_MODAL) hosting a single Compose view; touches outside the
 * capsule pass through to the app underneath.
 */
class CapsuleService : Service() {

    companion object {
        const val ACTION_STOP = "com.howck.dmi.action.STOP"
        private const val NOTIF_ID = 42
        private const val MEDIA_CHECK_INTERVAL_MS = 20_000L

        @Volatile
        var isRunning: Boolean = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var controller: MediaController? = null
    private var mediaCallback: MediaController.Callback? = null
    private var lastDestroyedToken: MediaSession.Token? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private val media = MediaState()

    @Volatile
    var batteryPct: Int = 100

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) batteryPct = level * 100 / scale
        }
    }

    private val mediaCheck = object : Runnable {
        override fun run() {
            if (!media.active) connectMedia()
            mainHandler.postDelayed(this, MEDIA_CHECK_INTERVAL_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        val batteryIntent = registerReceiver(
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        batteryIntent?.let { i ->
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) batteryPct = level * 100 / scale
        }

        mainHandler.postDelayed(mediaCheck, MEDIA_CHECK_INTERVAL_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification())
        mainHandler.post { installOverlay() }
        mainHandler.post { connectMedia() }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        releaseMedia()
        media.active = false
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        runCatching { unregisterReceiver(batteryReceiver) }
        stopForegroundCompat()
        isRunning = false
        super.onDestroy()
    }

    // ---------------------------------------------------------------------
    // Overlay window
    // ---------------------------------------------------------------------

    private fun installOverlay() {
        if (overlayView != null) return

        val compose = ComposeView(this).apply {
            isClickable = false
            focusable = false
            setContent {
                val scheme = dynamicColorSchemeFor(this@CapsuleService)
                MaterialTheme(colorScheme = scheme) {
                    CapsuleOverlay(
                        media = media,
                        batteryPct = this@CapsuleService.batteryPct,
                        onShortcut = { runShortcut(it) },
                        onMediaAction = { mediaAction(it) }
                    )
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        windowManager = getSystemService(WindowManager::class.java)
        windowManager?.addView(compose, params)
        overlayView = compose
    }

    // ---------------------------------------------------------------------
    // Media session
    // ---------------------------------------------------------------------

    private fun connectMedia() {
        releaseMedia()
        val manager = getSystemService(MediaSessionManager::class.java) ?: return
        val session = manager.activeSessions.maxByOrNull { it.sessionRank } ?: run {
            media.active = false
            return
        }
        if (session.sessionToken == lastDestroyedToken) {
            // Avoid an instant re-connect loop right after a session died.
            lastDestroyedToken = null
            media.active = false
            return
        }
        try {
            val candidate = MediaController(this, session.sessionToken, mainHandler)
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: Int) {
                    media.playing = state == MediaController.PLAYBACK_STATE_PLAYING
                }

                override fun onMetadataChanged(metadata: MediaMetadata?) {
                    media.title = metadata?.description?.title?.toString().orEmpty()
                    media.artist = metadata?.description?.artist?.toString().orEmpty()
                    media.durationMs =
                        metadata?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
                }

                override fun onSessionTimeTick(position: Long) {
                    media.positionMs = position
                }

                override fun onSessionDestroyed() {
                    lastDestroyedToken = session.sessionToken
                    media.active = false
                }
            }
            candidate.registerCallback(callback)
            candidate.connect()
            controller = candidate
            mediaCallback = callback
            media.active = true
        } catch (e: Exception) {
            media.active = false
        }
    }

    private fun releaseMedia() {
        val c = controller
        if (c != null) {
            mediaCallback?.let { cb -> runCatching { c.unregisterCallback(cb) } }
            runCatching { c.release() }
        }
        controller = null
        mediaCallback = null
    }

    private fun mediaAction(action: MediaAction) {
        val c = controller ?: return
        try {
            when (action) {
                MediaAction.PREV -> c.transportControls.skipToPrevious()
                MediaAction.NEXT -> c.transportControls.skipToNext()
                MediaAction.PLAY_PAUSE -> if (media.playing) c.pause() else c.play()
                is MediaAction.SEEK ->
                    c.seekTo((action.fraction * media.durationMs.coerceAtLeast(1L)).toLong())
            }
        } catch (e: Exception) {
            // Session may have gone away; the periodic re-check will recover.
        }
    }

    // ---------------------------------------------------------------------
    // Shortcuts
    // ---------------------------------------------------------------------

    private fun runShortcut(id: ShortcutId) {
        when (id) {
            ShortcutId.HIDE -> {
                stopForegroundCompat()
                stopSelf()
            }
            ShortcutId.CAMERA -> {
                val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    // Device without a launcher camera entry — ignore quietly.
                }
            }
            ShortcutId.PHONE -> {
                try {
                    startActivity(
                        Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: Exception) {
                    // No dialer available.
                }
            }
            ShortcutId.EMAIL -> {
                try {
                    startActivity(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: Exception) {
                    // No mail client installed.
                }
            }
            ShortcutId.SETTINGS -> {
                try {
                    startActivity(
                        Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                } catch (e: Exception) {
                    // Ignore.
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Notification / lifecycle helpers
    // ---------------------------------------------------------------------

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CapsuleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, DmiApp.CHANNEL_SERVICE)
            .setContentTitle(getString(R.string.notif_title_capsule_active))
            .setContentText(getString(R.string.notif_text_capsule_active))
            .setSmallIcon(R.drawable.ic_capsule_notif)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(openIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    getString(R.string.notif_action_exit),
                    stopIntent
                ).build()
            )
            .build()
    }

    private fun stopForegroundCompat() {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }
}
