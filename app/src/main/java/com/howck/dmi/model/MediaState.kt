package com.howck.dmi.model

/**
 * Quick actions available on the capsule.
 */
enum class ShortcutId {
    CAMERA,
    PHONE,
    EMAIL,
    SETTINGS,
    HIDE
}

/**
 * Media transport commands sent from the capsule UI.
 */
sealed class MediaAction {
    data object PREV : MediaAction()
    data object NEXT : MediaAction()
    data object PLAY_PAUSE : MediaAction()
    data class SEEK(val fraction: Float) : MediaAction()
}

/**
 * Snapshot of the active media session.
 *
 * Written from the [android.media.session.MediaController.Callback] (main looper)
 * and read by the overlay UI once per second. Fields are volatile to keep the
 * contract explicit even though both sides currently run on the main thread.
 */
class MediaState {
    @Volatile
    var active: Boolean = false

    @Volatile
    var playing: Boolean = false

    @Volatile
    var title: String = ""

    @Volatile
    var artist: String = ""

    @Volatile
    var positionMs: Long = 0L

    @Volatile
    var durationMs: Long = 0L
}
