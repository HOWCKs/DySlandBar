package com.howck.dmi.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.howck.dmi.BuildConfig
import com.howck.dmi.R
import com.howck.dmi.service.CapsuleService

/**
 * Main (settings/onboarding) screen: permission status, capsule start/stop,
 * feature overview.
 */
@Composable
fun DmiScreen() {
    val context = LocalContext.current

    val overlayLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val notifyLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notifGranted by remember { mutableStateOf(notificationsGranted(context)) }
    var serviceRunning by remember { mutableStateOf(CapsuleService.isRunning) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = Settings.canDrawOverlays(context)
                notifGranted = notificationsGranted(context)
                serviceRunning = CapsuleService.isRunning
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            context.getString(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                "v${BuildConfig.VERSION_NAME}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusCard(
                granted = overlayGranted,
                onGrant = {
                    overlayLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }
            )

            if (overlayGranted) {
                ServiceCard(
                    running = serviceRunning,
                    onStart = {
                        ContextCompat.startForegroundService(
                            context,
                            Intent(context, CapsuleService::class.java)
                        )
                    },
                    onStop = {
                        context.startService(
                            Intent(context, CapsuleService::class.java)
                                .setAction(CapsuleService.ACTION_STOP)
                        )
                    }
                )
                CardRow(
                    title = "Media controls",
                    subtitle = "Works with any app that exposes a MediaSession — " +
                        "Spotify, YouTube Music, Podcasts and more.",
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_music_note),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                ) { }
                CardRow(
                    title = "Quick shortcuts",
                    subtitle = "Camera, Phone, Email, Settings and Hide — one tap away from any screen.",
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                ) { }
            }

            NotificationCard(
                granted = notifGranted,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= 33) {
                        notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )

            AboutCard()
        }
    }
}

// -----------------------------------------------------------------------------
// Cards
// -----------------------------------------------------------------------------

@Composable
private fun CardRow(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    action: @Composable () -> Unit
) {
    OutlinedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            action()
        }
    }
}

@Composable
private fun StatusCard(granted: Boolean, onGrant: () -> Unit) {
    if (granted) {
        CardRow(
            title = "Overlay permission",
            subtitle = "Granted — the capsule can float above every app.",
            icon = {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        ) { }
    } else {
        CardRow(
            title = "Draw over other apps",
            subtitle = "Required so the capsule can float above every app. " +
                "You will be taken to system settings.",
            icon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        ) {
            Button(onClick = onGrant) { Text("Grant") }
        }
    }
}

@Composable
private fun ServiceCard(
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    CardRow(
        title = if (running) "Capsule is live" else "Capsule",
        subtitle = if (running) {
            "Look at the top of the screen. Tap the capsule to expand it, drag to move it."
        } else {
            "Start the floating capsule. It keeps running via a notification."
        },
        icon = {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    ) {
        if (running) {
            OutlinedButton(onClick = onStop) { Text("Hide") }
        } else {
            Button(onClick = onStart) { Text("Start") }
        }
    }
}

@Composable
private fun NotificationCard(granted: Boolean, onGrant: () -> Unit) {
    if (!granted) {
        CardRow(
            title = "Notifications",
            subtitle = "Used to keep the capsule alive in the background (Android requirement).",
            icon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        ) {
            Button(onClick = onGrant) { Text("Allow") }
        }
    }
}

@Composable
private fun AboutCard() {
    OutlinedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "Dynamic Material Island turns the area around your front camera into a " +
                    "living, expressive control surface. 100% on-device: no account, no " +
                    "analytics, no data leaves your phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Roadmap — next: flashlight with smooth brightness, accessibility gestures " +
                    "(swipe to scroll to top) and system-event notifications.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun notificationsGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < 33) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}
