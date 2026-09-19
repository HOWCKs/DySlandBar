package com.howck.dmi.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitPointerEvent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.howck.dmi.R
import com.howck.dmi.model.MediaAction
import com.howck.dmi.model.MediaState
import com.howck.dmi.model.ShortcutId
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * The floating capsule UI, hosted in a full-screen transparent overlay window.
 *
 * - Compact: a pill showing clock (or current track) + battery.
 * - Expanded: media transport controls + quick shortcut chips.
 * - Draggable anywhere on screen; tap toggles expansion with a spring.
 */
@Composable
fun CapsuleOverlay(
    media: MediaState,
    batteryPct: Int,
    onShortcut: (ShortcutId) -> Unit,
    onMediaAction: (MediaAction) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current

    var expanded by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(Offset.Unspecified) }
    var capsuleSize by remember { mutableStateOf(IntSize.Zero) }
    var screenSize by remember { mutableStateOf(IntSize.Zero) }
    var clock by remember { mutableStateOf(nowClock()) }
    var mediaTick by remember { mutableStateOf(0L) }

    val defaultTopPx = with(density) { 64.dp.toPx() }
    val defaultOffset: Offset = if (screenSize == IntSize.Zero) {
        Offset(0f, defaultTopPx)
    } else {
        Offset(
            x = ((screenSize.width - capsuleSize.width) / 2f).coerceAtLeast(0f),
            y = defaultTopPx
        )
    }
    val pos: Offset = if (offset == Offset.Unspecified) defaultOffset else offset

    // Tick once per second: refresh the clock and pull the latest media position
    // from the (main-thread) service state so the slider stays live.
    LaunchedEffect(Unit) {
        while (true) {
            clock = nowClock()
            mediaTick = media.positionMs
            delay(1_000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().onSizeChanged { screenSize = it }) {
        Box(
            modifier = Modifier
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .onSizeChanged { capsuleSize = it }
                .animateContentSize(spring(Spring.StiffnessMediumLow, dampingRatio = 0.8f))
                .clip(RoundedCornerShape(50))
                .background(colors.surfaceContainerHigh)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(50))
                .capsuleGesture(
                    onTap = { expanded = !expanded },
                    onDrag = { dx, dy ->
                        val current = if (offset == Offset.Unspecified) defaultOffset else offset
                        val maxX = (screenSize.width - capsuleSize.width).coerceAtLeast(0f)
                        val maxY = (screenSize.height - capsuleSize.height).coerceAtLeast(0f)
                        offset = Offset(
                            (current.x + dx).coerceIn(0f, maxX),
                            (current.y + dy).coerceIn(0f, maxY)
                        )
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 240.dp)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small "lens" dot — the capsule's nod to the camera cutout.
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(colors.tertiaryContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.onTertiaryContainer.copy(alpha = 0.55f))
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (media.active) media.title.ifBlank { "Media" } else clock,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (media.active) {
                        Icon(
                            painter = painterResource(R.drawable.ic_music_note),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "$batteryPct%",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurfaceVariant
                    )
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (media.active) {
                        MediaControls(media = media, onMediaAction = onMediaAction)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    ShortcutsRow(onShortcut = onShortcut)
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Media transport
// -----------------------------------------------------------------------------

@Composable
private fun MediaControls(
    media: MediaState,
    onMediaAction: (MediaAction) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val duration = if (media.durationMs > 0) media.durationMs else 1L
    val liveFraction = (media.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    var seekValue by remember { mutableFloatStateOf(liveFraction) }
    var dragging by remember { mutableStateOf(false) }

    // Keep the slider synced with live playback unless the user is dragging it.
    LaunchedEffect(liveFraction) {
        if (!dragging) seekValue = liveFraction
    }

    Row(
        modifier = Modifier.width(300.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onMediaAction(MediaAction.PREV) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_previous),
                contentDescription = "Previous",
                tint = colors.onSurface
            )
        }
        Surface(
            onClick = { onMediaAction(MediaAction.PLAY_PAUSE) },
            shape = CircleShape,
            color = colors.primary,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (media.playing) {
                    Icon(
                        painter = painterResource(R.drawable.ic_pause),
                        contentDescription = "Pause",
                        modifier = Modifier.size(22.dp),
                        tint = colors.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(22.dp),
                        tint = colors.onPrimary
                    )
                }
            }
        }
        IconButton(
            onClick = { onMediaAction(MediaAction.NEXT) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_next),
                contentDescription = "Next",
                tint = colors.onSurface
            )
        }
        Slider(
            value = seekValue,
            onValueChange = {
                dragging = true
                seekValue = it
            },
            onValueChangeFinished = {
                dragging = false
                onMediaAction(MediaAction.SEEK(seekValue))
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

// -----------------------------------------------------------------------------
// Quick shortcuts
// -----------------------------------------------------------------------------

@Composable
private fun ShortcutsRow(onShortcut: (ShortcutId) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ShortcutChip("Camera", ShortcutId.CAMERA, onShortcut) {
            Icon(
                painter = painterResource(R.drawable.ic_camera),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        ShortcutChip("Phone", ShortcutId.PHONE, onShortcut) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        ShortcutChip("Email", ShortcutId.EMAIL, onShortcut) {
            Icon(
                imageVector = Icons.Filled.Email,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        ShortcutChip("Settings", ShortcutId.SETTINGS, onShortcut) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        ShortcutChip("Hide", ShortcutId.HIDE, onShortcut) {
            Icon(
                imageVector = Icons.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ShortcutChip(
    label: String,
    id: ShortcutId,
    onShortcut: (ShortcutId) -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = { onShortcut(id) },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 1.dp,
        modifier = Modifier.heightIn(min = 40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

// -----------------------------------------------------------------------------
// Gesture: tap toggles expansion, drag moves the capsule window position.
// -----------------------------------------------------------------------------

private fun Modifier.capsuleGesture(
    onTap: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit
): Modifier = pointerInput(Unit) {
    val slop = 14.dp.toPx()
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var moved = false
        var previous: Offset? = null
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull() ?: break
            if (change.isUp) break
            val current = change.position
            val prev = previous ?: current
            previous = current
            val dx = current.x - prev.x
            val dy = current.y - prev.y
            if (!moved && hypot(dx, dy) > slop) moved = true
            if (moved) {
                onDrag(dx, dy)
                change.consume()
            }
        }
        if (!moved) onTap()
    }
}

private fun nowClock(): String {
    val now = LocalTime.now()
    return "%02d:%02d".format(now.hour, now.minute)
}
