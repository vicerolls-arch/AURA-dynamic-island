package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.IncomingCall
import com.example.model.IslandConfig
import com.example.model.IslandMode
import com.example.model.IslandNotification
import com.example.model.IslandShape
import com.example.model.MediaTrack
import com.example.model.PerformanceMode
import com.example.model.TimerState
import com.example.ui.theme.IslandBlack
import com.example.util.DeviceCapability
import com.example.util.DeviceTier
import com.example.util.HapticHelper
import com.example.util.HapticType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Data class representing the visual dimensions of the Dynamic Island.
 * Used for updateTransition to synchronize width, height, and corner radius.
 */
private data class IslandGeometryState(
    val width: Dp,
    val height: Dp,
    val cornerRadius: Dp
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DynamicIslandView(
    mode: IslandMode,
    isExpanded: Boolean,
    config: IslandConfig,
    mediaTrack: MediaTrack,
    incomingCall: IncomingCall,
    notification: IslandNotification,
    timerState: TimerState,
    chargingPercentage: Int,
    customMessage: String,
    onIslandClick: () -> Unit,
    onTogglePlayback: () -> Unit,
    onCollapse: () -> Unit,
    applyPositionOffset: Boolean = true,
    isCustomizationPreviewActive: Boolean = false,
    isNotificationDetailExpanded: Boolean = false,
    onExpandNotificationDetail: () -> Unit = {},
    isMediaDetailExpanded: Boolean = false,
    onExpandMediaDetail: () -> Unit = {},
    onIslandDoubleClick: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSendReply: (key: String, text: String) -> Unit = { _, _ -> },
    onSilenceRinger: () -> Unit = {},
    onAcceptCall: () -> Unit = {},
    onDeclineCall: () -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    secondaryMode: IslandMode? = null,
    onSecondaryClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val deviceTier = remember(context) { DeviceCapability.detectTier(context) }

    val isIdle = !isExpanded && mode == IslandMode.COMPACT && !mediaTrack.isPlaying && !isCustomizationPreviewActive

    // Target Dimensions mapping state machine
    val targetWidth = if (isIdle) {
        when (config.islandShape) {
            IslandShape.DOT_EXPAND -> 12.dp
            IslandShape.BROAD_DOCK -> (config.widthDp * 1.15f).dp.coerceAtLeast(180.dp)
            IslandShape.NOTCH -> 120.dp
        }
    } else if (!isExpanded) {
        when (config.islandShape) {
            IslandShape.DOT_EXPAND -> config.widthDp.dp
            IslandShape.BROAD_DOCK -> (config.widthDp * 1.25f).dp.coerceAtLeast(200.dp)
            IslandShape.NOTCH -> config.widthDp.dp
        }
    } else {
        when (mode) {
            IslandMode.COMPACT -> config.widthDp.dp
            IslandMode.MEDIA -> if (isMediaDetailExpanded) 330.dp else 140.dp
            IslandMode.CALL -> 320.dp
            IslandMode.NOTIFICATION -> if (isNotificationDetailExpanded) 320.dp else 180.dp
            IslandMode.CHARGING -> 240.dp
            IslandMode.TIMER -> 290.dp
            IslandMode.CUSTOM_TEXT -> 290.dp
        }
    }

    val targetHeight = if (isIdle) {
        when (config.islandShape) {
            IslandShape.DOT_EXPAND -> 12.dp
            IslandShape.BROAD_DOCK -> 22.dp
            IslandShape.NOTCH -> 26.dp
        }
    } else if (!isExpanded) {
        when (config.islandShape) {
            IslandShape.DOT_EXPAND -> config.heightDp.dp
            IslandShape.BROAD_DOCK -> (config.heightDp * 0.85f).dp.coerceAtLeast(28.dp)
            IslandShape.NOTCH -> (config.heightDp + 4).dp
        }
    } else {
        when (mode) {
            IslandMode.COMPACT -> config.heightDp.dp
            IslandMode.MEDIA -> if (isMediaDetailExpanded) 168.dp else 40.dp
            IslandMode.CALL -> 90.dp
            IslandMode.NOTIFICATION -> if (isNotificationDetailExpanded) {
                if (notification.canReply) 140.dp else 100.dp
            } else 44.dp
            IslandMode.CHARGING -> 56.dp
            IslandMode.TIMER -> 86.dp
            IslandMode.CUSTOM_TEXT -> 64.dp
        }
    }

    val targetCornerRadius = when (config.islandShape) {
        IslandShape.NOTCH -> if (isIdle) 14.dp else if (isExpanded) 28.dp else config.cornerRadiusDp.dp
        IslandShape.BROAD_DOCK -> if (isIdle) 11.dp else if (isExpanded) 28.dp else config.cornerRadiusDp.dp
        IslandShape.DOT_EXPAND -> if (isIdle) 6.dp else if (isExpanded) 32.dp else config.cornerRadiusDp.dp
    }

    // Single unified transition for synchronized width, height, and cornerRadius
    val geometryState = remember(targetWidth, targetHeight, targetCornerRadius) {
        IslandGeometryState(targetWidth, targetHeight, targetCornerRadius)
    }

    val geometryTransition = updateTransition(
        targetState = geometryState,
        label = "islandGeometry"
    )

    // Animated geometry properties mapping to customized interruptible spring specs
    val animatedWidth by geometryTransition.animateDp(
        transitionSpec = {
            if (config.reduceMotion) {
                tween(durationMillis = 120, easing = LinearOutSlowInEasing)
            } else if (deviceTier == DeviceTier.LOW) {
                // Snappy, lightweight spring
                spring(stiffness = 500f, dampingRatio = Spring.DampingRatioNoBouncy)
            } else {
                if (targetState.width > initialState.width) {
                    // Expanding: bouncy and fluid
                    spring(stiffness = 300f, dampingRatio = 0.72f)
                } else {
                    // Collapsing: fast and tight
                    spring(stiffness = 500f, dampingRatio = Spring.DampingRatioNoBouncy)
                }
            }
        },
        label = "islandWidth"
    ) { it.width }

    val animatedHeight by geometryTransition.animateDp(
        transitionSpec = {
            if (config.reduceMotion) {
                tween(durationMillis = 120, easing = LinearOutSlowInEasing)
            } else if (deviceTier == DeviceTier.LOW) {
                spring(stiffness = 500f, dampingRatio = Spring.DampingRatioNoBouncy)
            } else {
                if (targetState.height > initialState.height) {
                    spring(stiffness = 300f, dampingRatio = 0.72f)
                } else {
                    spring(stiffness = 500f, dampingRatio = Spring.DampingRatioNoBouncy)
                }
            }
        },
        label = "islandHeight"
    ) { it.height }

    val animatedCornerRadius by geometryTransition.animateDp(
        transitionSpec = {
            if (config.reduceMotion) {
                tween(durationMillis = 120, easing = LinearOutSlowInEasing)
            } else if (deviceTier == DeviceTier.LOW) {
                spring(stiffness = 500f, dampingRatio = Spring.DampingRatioNoBouncy)
            } else {
                if (targetState.cornerRadius > initialState.cornerRadius) {
                    spring(stiffness = 300f, dampingRatio = 0.72f)
                } else {
                    spring(stiffness = 500f, dampingRatio = Spring.DampingRatioNoBouncy)
                }
            }
        },
        label = "islandCornerRadius"
    ) { it.cornerRadius }

    val pillShape = remember(config.islandShape, animatedCornerRadius) {
        if (config.islandShape == IslandShape.NOTCH) {
            RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = animatedCornerRadius,
                bottomEnd = animatedCornerRadius
            )
        } else {
            RoundedCornerShape(animatedCornerRadius)
        }
    }

    val islandBgColor = remember(config.backgroundColorHex, config.backgroundAlpha) {
        try {
            val parsed = android.graphics.Color.parseColor(config.backgroundColorHex)
            Color(parsed).copy(alpha = config.backgroundAlpha.coerceIn(0.1f, 1.0f))
        } catch (e: Exception) {
            IslandBlack.copy(alpha = config.backgroundAlpha.coerceIn(0.1f, 1.0f))
        }
    }

    // Touch down press scaling state
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(stiffness = 600f, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "pressScale"
    )

    // Keyline tint border color calculation based on app brand color / active state
    val keylineColor = remember(mode, notification.tintColor, mediaTrack.isPlaying) {
        if (mode == IslandMode.NOTIFICATION && notification.tintColor != null) {
            Color(notification.tintColor).copy(alpha = 0.40f)
        } else if (mode == IslandMode.MEDIA && mediaTrack.isPlaying) {
            Color(0xFF00FFB2).copy(alpha = 0.20f)
        } else {
            Color.White.copy(alpha = 0.08f)
        }
    }

    Box(
        modifier = modifier
            .let {
                if (applyPositionOffset) {
                    it.offset { IntOffset(config.offsetXDp.dp.roundToPx(), config.offsetYDp.dp.roundToPx()) }
                } else {
                    it
                }
            }
            .testTag("dynamic_island_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(animatedHeight)
                    .scale(pressScale)
                    .clip(pillShape)
                    .background(islandBgColor)
                    .border(
                        width = 0.75.dp,
                        color = keylineColor,
                        shape = pillShape
                    )
                    .pointerInput(onIslandClick, onIslandDoubleClick, onSwipeLeft, onSwipeRight) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            var isDrag = false
                            var accumulatedX = 0f
                            var longPressed = false

                            val longPressJob = coroutineScope.launch {
                                delay(viewConfiguration.longPressTimeoutMillis)
                                longPressed = true
                                isPressed = false
                                if (config.vibrationFeedback) {
                                    HapticHelper.trigger(context, HapticType.IMPACT_MEDIUM)
                                }
                                // Long press triggers expanding / detail panel
                                onIslandClick()
                            }

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.pressed) {
                                    val dragAmount = change.position.x - change.previousPosition.x
                                    if (kotlin.math.abs(change.position.x - down.position.x) > 18f) {
                                        isDrag = true
                                        longPressJob.cancel()
                                    }
                                    if (isDrag) {
                                        accumulatedX += dragAmount
                                        change.consume()
                                    }
                                } else {
                                    break
                                }
                            } while (true)

                            longPressJob.cancel()
                            isPressed = false

                            if (isDrag) {
                                if (accumulatedX < -80f) {
                                    if (config.vibrationFeedback) {
                                        HapticHelper.trigger(context, HapticType.IMPACT_LIGHT)
                                    }
                                    onSwipeLeft()
                                } else if (accumulatedX > 80f) {
                                    if (config.vibrationFeedback) {
                                        HapticHelper.trigger(context, HapticType.IMPACT_LIGHT)
                                    }
                                    onSwipeRight()
                                }
                            } else if (!longPressed) {
                                // Short tap: deep-link opens source app
                                if (config.vibrationFeedback) {
                                    HapticHelper.trigger(context, HapticType.IMPACT_LIGHT)
                                }
                                onIslandDoubleClick()
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("dynamic_island_pill"),
                contentAlignment = Alignment.Center
            ) {
                val animState = remember(mode, isExpanded) {
                    if (isExpanded) mode else {
                        if (mode != IslandMode.COMPACT) mode else IslandMode.COMPACT
                    }
                }

                AnimatedContent(
                    targetState = animState,
                    transitionSpec = {
                        val toExpanded = isExpanded
                        if (toExpanded) {
                            // iOS-like emergent slide up from bottom edge
                            (slideInVertically(
                                animationSpec = spring(stiffness = 320f, dampingRatio = 0.78f),
                                initialOffsetY = { it / 3 }
                            ) + fadeIn(spring(stiffness = 400f))).with(
                                fadeOut(tween(80))
                            )
                        } else {
                            // Snappy drop down collapse
                            fadeIn(tween(60)).with(
                                slideOutVertically(
                                    animationSpec = spring(stiffness = 500f, dampingRatio = Spring.DampingRatioNoBouncy),
                                    targetOffsetY = { it / 3 }
                                ) + fadeOut(tween(80))
                            )
                        }
                    },
                    label = "islandContent"
                ) { currentMode ->
                    when (currentMode) {
                        IslandMode.COMPACT -> {
                            CompactIslandContent(mediaTrack = mediaTrack)
                        }
                        else -> {
                            if (isExpanded) {
                                when (currentMode) {
                                    IslandMode.MEDIA -> {
                                        if (isMediaDetailExpanded) {
                                            ExpandedMediaContent(
                                                track = mediaTrack,
                                                onTogglePlayback = onTogglePlayback,
                                                onSkipNext = onSkipNext,
                                                onSkipPrevious = onSkipPrevious,
                                                onCollapse = onCollapse
                                            )
                                        } else {
                                            MediaMiniVisualizer(
                                                mediaTrack = mediaTrack,
                                                onTap = onExpandMediaDetail
                                            )
                                        }
                                    }
                                    IslandMode.CALL -> {
                                        ExpandedCallContent(
                                            call = incomingCall,
                                            onAcceptCall = onAcceptCall,
                                            onDeclineCall = onDeclineCall,
                                            onSilenceRinger = onSilenceRinger,
                                            onCollapse = onCollapse
                                        )
                                    }
                                    IslandMode.NOTIFICATION -> {
                                        if (isNotificationDetailExpanded) {
                                            ExpandedNotificationContent(
                                                notification = notification,
                                                onSendReply = onSendReply,
                                                onCollapse = onCollapse
                                            )
                                        } else {
                                            NotificationMiniPeek(
                                                notification = notification,
                                                onTap = onExpandNotificationDetail
                                            )
                                        }
                                    }
                                    IslandMode.CHARGING -> {
                                        ExpandedChargingContent(percentage = chargingPercentage)
                                    }
                                    IslandMode.TIMER -> {
                                        ExpandedTimerContent(
                                            timerState = timerState,
                                            onPause = onPauseTimer,
                                            onResume = onResumeTimer,
                                            onReset = onResetTimer
                                        )
                                    }
                                    IslandMode.COMPACT -> {}
                                    IslandMode.CUSTOM_TEXT -> {
                                        ExpandedCustomTextContent(message = customMessage)
                                    }
                                }
                            } else {
                                // COMPACT-LIVE STATE (Split leading/trailing presentation)
                                CompactLiveContent(
                                    mode = currentMode,
                                    mediaTrack = mediaTrack,
                                    notification = notification,
                                    timerState = timerState,
                                    chargingPercentage = chargingPercentage,
                                    customMessage = customMessage
                                )
                            }
                        }
                    }
                }
            }

            if (secondaryMode != null && secondaryMode != IslandMode.COMPACT) {
                Box(
                    modifier = Modifier
                        .size(animatedHeight)
                        .clip(CircleShape)
                        .background(islandBgColor)
                        .border(0.75.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                        .clickable { onSecondaryClick() }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (secondaryMode) {
                        IslandMode.MEDIA -> {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFF00FFB2),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IslandMode.TIMER -> {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IslandMode.CALL -> {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = Color(0xFF30D158),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IslandMode.NOTIFICATION -> {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactIslandContent(mediaTrack: MediaTrack) {
    if (mediaTrack.isPlaying) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mediaTrack.albumArtRes != null) {
                Image(
                    painter = painterResource(id = mediaTrack.albumArtRes),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                )
            }

            // High-quality fluid audio visualizer waveform on the trailing side
            AudioWaveform(isPlaying = mediaTrack.isPlaying)
        }
    } else {
        IdleDot()
    }
}

@Composable
private fun IdleDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.85f))
    )
}

@Composable
private fun AudioWaveform(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
    
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 0.60f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )
    val bar5Height by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar5"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(14.dp)
    ) {
        val h1 = if (isPlaying) bar1Height else 0.2f
        val h2 = if (isPlaying) bar2Height else 0.4f
        val h3 = if (isPlaying) bar3Height else 0.5f
        val h4 = if (isPlaying) bar4Height else 0.3f
        val h5 = if (isPlaying) bar5Height else 0.2f

        val waveformColor = Color(0xFF00FFB2)

        Box(modifier = Modifier.width(2.dp).height(12.dp * h1).clip(CircleShape).background(waveformColor))
        Box(modifier = Modifier.width(2.dp).height(12.dp * h2).clip(CircleShape).background(waveformColor))
        Box(modifier = Modifier.width(2.dp).height(12.dp * h3).clip(CircleShape).background(waveformColor))
        Box(modifier = Modifier.width(2.dp).height(12.dp * h4).clip(CircleShape).background(waveformColor))
        Box(modifier = Modifier.width(2.dp).height(12.dp * h5).clip(CircleShape).background(waveformColor))
    }
}

@Composable
private fun CompactLiveContent(
    mode: IslandMode,
    mediaTrack: MediaTrack,
    notification: IslandNotification,
    timerState: TimerState,
    chargingPercentage: Int,
    customMessage: String
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // LEADING (Pill Left)
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.CenterStart
        ) {
            when (mode) {
                IslandMode.MEDIA -> {
                    if (mediaTrack.albumArtRes != null) {
                        Image(
                            painter = painterResource(id = mediaTrack.albumArtRes),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.8f))
                        )
                    }
                }
                IslandMode.NOTIFICATION -> {
                    val appIconBitmap = remember(notification.appIcon) {
                        notification.appIcon?.let {
                            try { it.toBitmap().asImageBitmap() } catch (e: Exception) { null }
                        }
                    }
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = notification.appName.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IslandMode.CALL -> {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IslandMode.CHARGING -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color(0xFF30D158),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IslandMode.TIMER -> {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(16.dp)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00FFB2))
                    )
                }
            }
        }

        // TRAILING (Pill Right)
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.CenterEnd
        ) {
            when (mode) {
                IslandMode.MEDIA -> {
                    AudioWaveform(isPlaying = mediaTrack.isPlaying)
                }
                IslandMode.NOTIFICATION -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759))
                    )
                }
                IslandMode.CALL -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "callBlink")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "blinkAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF30D158).copy(alpha = alpha))
                    )
                }
                IslandMode.CHARGING -> {
                    Text(
                        text = "$chargingPercentage%",
                        color = Color(0xFF30D158),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IslandMode.TIMER -> {
                    Text(
                        text = formatSeconds(timerState.remainingSeconds),
                        color = Color(0xFFFF9500),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IslandMode.CUSTOM_TEXT -> {
                    Text(
                        text = customMessage.take(12),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ExpandedMediaContent(
    track: MediaTrack,
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onCollapse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (track.albumArtRes != null) {
                    Image(
                        painter = painterResource(id = track.albumArtRes),
                        contentDescription = "Cover",
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2A2A2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = Color(0xFFC4C7C8),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            AudioWaveform(isPlaying = track.isPlaying)
        }

        Column {
            val progress = track.currentPositionSeconds.toFloat() / track.durationSeconds.toFloat().coerceAtLeast(1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatSeconds(track.currentPositionSeconds),
                    color = Color(0xFF8E9192),
                    fontSize = 10.sp
                )
                Text(
                    text = formatSeconds(track.durationSeconds),
                    color = Color(0xFF8E9192),
                    fontSize = 10.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSkipPrevious, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Track", tint = Color.White)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClick = onTogglePlayback),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (track.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play Pause",
                    tint = IslandBlack,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onSkipNext, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Track", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ExpandedCallContent(
    call: IncomingCall,
    onAcceptCall: () -> Unit,
    onDeclineCall: () -> Unit,
    onSilenceRinger: () -> Unit,
    onCollapse: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "callPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "avatarPulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "ringAlpha"
    )

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Caller info with pulsing avatar ring
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(contentAlignment = Alignment.Center) {
                // Outer pulse ring
                Box(
                    modifier = Modifier
                        .size((40 * pulseScale).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158).copy(alpha = pulseAlpha))
                )
                // Avatar
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = call.callerName.take(1).uppercase(),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = call.callerName, color = Color.White,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(text = call.callType, color = Color(0xFF30D158), fontSize = 11.sp)
            }
        }

        // Accept / Decline action pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Decline — red pill
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFFFF3B30))
                    .clickable { onDeclineCall(); onCollapse() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Decline",
                    tint = Color.White, modifier = Modifier.size(18.dp)
                )
            }
            // Accept — green pill
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(Color(0xFF30D158))
                    .clickable { onAcceptCall(); onCollapse() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Accept",
                    tint = Color.White, modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedNotificationContent(
    notification: IslandNotification,
    onSendReply: (key: String, text: String) -> Unit,
    onCollapse: () -> Unit
) {
    var replyText by remember { mutableStateOf("") }
    val appIconBitmap = remember(notification.appIcon) {
        notification.appIcon?.let { drawable ->
            try {
                drawable.toBitmap().asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (appIconBitmap != null) {
                    Image(
                        bitmap = appIconBitmap,
                        contentDescription = notification.appName,
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF25D366)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = notification.appName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = notification.appName,
                    color = Color(0xFF8E9192),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = notification.timeFormatted,
                color = Color(0xFF8E9192),
                fontSize = 11.sp
            )
        }

        Column {
            Text(
                text = notification.sender,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = notification.message,
                color = Color(0xFFE5E2E1),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (notification.canReply) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (replyText.isEmpty()) {
                                Text(
                                    text = "Reply...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (replyText.isNotBlank()) Color(0xFF00FFB2) else Color.White.copy(alpha = 0.2f))
                        .clickable(enabled = replyText.isNotBlank()) {
                            onSendReply(notification.notificationKey, replyText)
                            replyText = ""
                            onCollapse()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Reply",
                        tint = if (replyText.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedChargingContent(percentage: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "chargingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759).copy(alpha = pulseAlpha * 0.3f))
                    .border(1.dp, Color(0xFF34C759).copy(alpha = pulseAlpha), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Charging Active",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Fast Charging",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Ultra SuperCharge 65W",
                    color = Color(0xFF8E9192),
                    fontSize = 10.sp
                )
            }
        }

        Text(
            text = "$percentage%",
            color = Color(0xFF34C759),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExpandedTimerContent(
    timerState: TimerState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit
) {
    val progress = if (timerState.totalSeconds > 0)
        timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
    else 0f

    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular arc progress ring + time display
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(52.dp)) {
                val strokeWidth = 4.dp.toPx()
                val sweepAngle = 360f * progress
                drawArc(
                    color = Color(0xFF3D3D3D),
                    startAngle = -90f, sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                drawArc(
                    color = Color(0xFFFF9500),
                    startAngle = -90f, sweepAngle = sweepAngle,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
            Text(
                text = formatSeconds(timerState.remainingSeconds),
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }

        // Label
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(text = timerState.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = if (timerState.isRunning) "Running" else "Paused",
                color = if (timerState.isRunning) Color(0xFFFF9500) else Color(0xFF8E9192),
                fontSize = 10.sp
            )
        }

        // Controls: Pause/Resume + Reset
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Reset
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable(onClick = onReset),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Replay, contentDescription = "Reset", tint = Color.White, modifier = Modifier.size(15.dp))
            }
            // Pause / Resume
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape)
                    .background(Color(0xFFFF9500))
                    .clickable(onClick = if (timerState.isRunning) onPause else onResume),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (timerState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (timerState.isRunning) "Pause" else "Resume",
                    tint = Color.Black, modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedCustomTextContent(message: String) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF00FFB2))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun NotificationMiniPeek(notification: IslandNotification, onTap: () -> Unit) {
    val imageBitmap = remember(notification.appIcon) {
        notification.appIcon?.let {
            try {
                it.toBitmap().asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFF34C759)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(7.dp)
            )
        }
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = notification.appName,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
            )
        }
        Text(
            text = notification.appName,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MediaMiniVisualizer(mediaTrack: MediaTrack, onTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (mediaTrack.albumArtRes != null) {
                Image(
                    painter = painterResource(id = mediaTrack.albumArtRes),
                    contentDescription = mediaTrack.title,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                )
            }
            Text(
                text = mediaTrack.title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AudioWaveform(isPlaying = mediaTrack.isPlaying)
    }
}

private fun formatSeconds(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
