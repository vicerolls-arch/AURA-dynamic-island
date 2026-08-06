package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.outlined.Dvr
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AlertHistoryEntity
import com.example.model.IncomingCall
import com.example.model.IslandConfig
import com.example.model.IslandMode
import com.example.model.IslandNotification
import com.example.model.MediaTrack
import com.example.model.TimerState
import com.example.ui.components.CrowdCanvas
import com.example.ui.components.DynamicIslandView
import com.example.ui.theme.ObsidianBackground
import com.example.util.DeviceCapability
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BottomSheetTab {
    NOTIFICATIONS,
    STATUS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    islandMode: IslandMode,
    isExpanded: Boolean,
    config: IslandConfig,
    mediaTrack: MediaTrack,
    incomingCall: IncomingCall,
    notification: IslandNotification,
    timerState: TimerState,
    chargingPercentage: Int,
    customMessage: String,
    secondaryIslandMode: IslandMode? = null,
    pendingNotificationCount: Int = 0,
    pendingNextNotification: IslandNotification? = null,
    onSecondaryClick: () -> Unit = {},
    isNotificationDetailExpanded: Boolean = false,
    onExpandNotificationDetail: () -> Unit = {},
    isMediaDetailExpanded: Boolean = false,
    onExpandMediaDetail: () -> Unit = {},
    onIslandClick: () -> Unit,
    onIslandDoubleClick: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onTogglePlayback: () -> Unit,
    onSkipNext: () -> Unit = {},
    onSkipPrevious: () -> Unit = {},
    onSendReply: (key: String, text: String) -> Unit = { _, _ -> },
    onSilenceRinger: () -> Unit = {},
    onAcceptCall: () -> Unit = {},
    onDeclineCall: () -> Unit = {},
    onPauseTimer: () -> Unit = {},
    onResumeTimer: () -> Unit = {},
    onResetTimer: () -> Unit = {},
    onCollapse: () -> Unit,
    onQuickModuleClick: (IslandMode) -> Unit,
    onLaunchOverlay: () -> Unit = {},
    onClearAlertsHistory: () -> Unit = {},
    onDeleteAlert: (Long) -> Unit = {},
    alertsHistory: List<AlertHistoryEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val flatNotif = remember { Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") }
    val isNotifListenerActive = remember(flatNotif) { flatNotif != null && flatNotif.contains(context.packageName) }
    val isOverlayActive = config.overlayServiceEnabled

    var showStatusBottomSheet by remember { mutableStateOf(false) }

    val hasUnattendedIssue = remember(isOverlayActive, isNotifListenerActive, alertsHistory) {
        !isOverlayActive || !isNotifListenerActive || alertsHistory.isNotEmpty()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("home_screen_container")
    ) {
        // Moving Crowd Canvas on background
        CrowdCanvas(
            modifier = Modifier.fillMaxSize(),
            peepCount = 18,
            enabled = !config.reduceMotion
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar with centered "AURA" title and top-right minimalist alert icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = "AURA",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("aura_title_header")
                )

                // Minimalist Alert Icon on Top Right
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .clickable { showStatusBottomSheet = true }
                        .padding(4.dp)
                        .testTag("status_alert_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Status & Failure Alerts",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        if (hasUnattendedIssue) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Quick interactive launcher pills on home screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 120.dp)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickTriggerPill(
                        label = "Music",
                        icon = Icons.Default.MusicNote,
                        onClick = { onQuickModuleClick(IslandMode.MEDIA) }
                    )
                    QuickTriggerPill(
                        label = "Call",
                        icon = Icons.Default.PhoneInTalk,
                        onClick = { onQuickModuleClick(IslandMode.CALL) }
                    )
                    QuickTriggerPill(
                        label = "Alert",
                        icon = Icons.Default.Notifications,
                        onClick = { onQuickModuleClick(IslandMode.NOTIFICATION) }
                    )
                    QuickTriggerPill(
                        label = "Pause",
                        icon = Icons.Default.PauseCircle,
                        onClick = { onSwipeLeft() }
                    )
                }

                // Launch floating button on the bottom right side
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .clickable(onClick = onLaunchOverlay)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("launch_overlay_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = "Launch Overlay",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Launch",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Dynamic Island Capsule / Pill preview positioned to match real overlay coordinates
        DynamicIslandView(
            mode = islandMode,
            isExpanded = isExpanded,
            config = config,
            mediaTrack = mediaTrack,
            incomingCall = incomingCall,
            notification = notification,
            timerState = timerState,
            chargingPercentage = chargingPercentage,
            customMessage = customMessage,
            isNotificationDetailExpanded = isNotificationDetailExpanded,
            onExpandNotificationDetail = onExpandNotificationDetail,
            isMediaDetailExpanded = isMediaDetailExpanded,
            onExpandMediaDetail = onExpandMediaDetail,
            onIslandClick = onIslandClick,
            onIslandDoubleClick = onIslandDoubleClick,
            onSwipeLeft = onSwipeLeft,
            onSwipeRight = onSwipeRight,
            onTogglePlayback = onTogglePlayback,
            onSkipNext = onSkipNext,
            onSkipPrevious = onSkipPrevious,
            onSendReply = onSendReply,
            onSilenceRinger = onSilenceRinger,
            onAcceptCall = onAcceptCall,
            onDeclineCall = onDeclineCall,
            onPauseTimer = onPauseTimer,
            onResumeTimer = onResumeTimer,
            onResetTimer = onResetTimer,
            onCollapse = onCollapse,
            secondaryMode = secondaryIslandMode,
            pendingNotificationCount = pendingNotificationCount,
            pendingNextNotification = pendingNextNotification,
            onSecondaryClick = onSecondaryClick,
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.TopCenter)
        )
    }

    // App Status & System Alerts Bottom Sheet
    if (showStatusBottomSheet) {
        var sheetTab by remember { mutableStateOf(BottomSheetTab.NOTIFICATIONS) }

        ModalBottomSheet(
            onDismissRequest = { showStatusBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF141416),
            contentColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3A3A3C))
                )
            },
            modifier = Modifier.testTag("status_info_bottom_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Small Pill Navigation Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF222224))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                            .padding(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Notifications Pill Tab
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (sheetTab == BottomSheetTab.NOTIFICATIONS) Color.White else Color.Transparent)
                                    .clickable { sheetTab = BottomSheetTab.NOTIFICATIONS }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notifications",
                                        tint = if (sheetTab == BottomSheetTab.NOTIFICATIONS) Color.Black else Color(0xFF8E9192),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Notifications",
                                        color = if (sheetTab == BottomSheetTab.NOTIFICATIONS) Color.Black else Color(0xFF8E9192),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // App Status Pill Tab
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (sheetTab == BottomSheetTab.STATUS) Color.White else Color.Transparent)
                                    .clickable { sheetTab = BottomSheetTab.STATUS }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Dvr,
                                        contentDescription = "App Status",
                                        tint = if (sheetTab == BottomSheetTab.STATUS) Color.Black else Color(0xFF8E9192),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "App Status",
                                        color = if (sheetTab == BottomSheetTab.STATUS) Color.Black else Color(0xFF8E9192),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (sheetTab == BottomSheetTab.NOTIFICATIONS) {
                    // Notifications Page inside Bottom Sheet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recent Notifications",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${alertsHistory.size} total logs",
                                color = Color(0xFF8E9192),
                                fontSize = 12.sp
                            )
                        }

                        if (alertsHistory.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .clickable { onClearAlertsHistory() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (alertsHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1E20))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = Color(0xFF555558),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No recent notifications recorded",
                                    color = Color(0xFF8E9192),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            alertsHistory.take(15).forEach { alert ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1E1E20))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = alert.appName,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = alert.message,
                                                color = Color(0xFFA0A0A2),
                                                fontSize = 11.sp,
                                                maxLines = 2
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alert.timestamp)),
                                                color = Color(0xFF6C6E70),
                                                fontSize = 10.sp
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .clickable { onDeleteAlert(alert.id) }
                                                    .padding(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = Color(0xFF8E9192),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // App Status Page inside Bottom Sheet
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "App Status & Core Services",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (hasUnattendedIssue) "Action required for full functionality" else "All core services operational",
                                color = if (hasUnattendedIssue) Color(0xFFFF9500) else Color(0xFF30D158),
                                fontSize = 12.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .clickable {
                                    showStatusBottomSheet = false
                                    onLaunchOverlay()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Restart Services",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusDetailItem(
                            title = "Overlay Window Service",
                            subtitle = if (isOverlayActive) "Active & drawing dynamic island" else "Service stopped or paused",
                            isActive = isOverlayActive,
                            actionText = if (isOverlayActive) "Restart" else "Start Service",
                            onAction = {
                                showStatusBottomSheet = false
                                onLaunchOverlay()
                            }
                        )

                        StatusDetailItem(
                            title = "Notification Listener Bridge",
                            subtitle = if (isNotifListenerActive) "Connected • Intercepting status updates" else "Permission missing in System Settings",
                            isActive = isNotifListenerActive,
                            actionText = if (isNotifListenerActive) "Settings" else "Grant Permission",
                            onAction = {
                                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "HARDWARE CAPABILITY & REFRESH RATE",
                            color = Color(0xFF8E9192),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E1E20))
                                .padding(12.dp)
                        ) {
                            val tier = remember(context) { DeviceCapability.detectTier(context) }
                            val hz = remember(context) { DeviceCapability.detectRefreshRateHz(context) }
                            Column {
                                Text(
                                    text = "Device Tier: ${tier.name} (${hz.toInt()}Hz)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when (tier) {
                                        com.example.util.DeviceTier.HIGH -> "Full spring physics & high refresh rate enabled"
                                        com.example.util.DeviceTier.MEDIUM -> "Damped spring animations for optimal performance"
                                        com.example.util.DeviceTier.LOW -> "Calm tween transitions for smooth low-RAM execution"
                                    },
                                    color = Color(0xFF8E9192),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDetailItem(
    title: String,
    subtitle: String,
    isActive: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E20))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color(0xFF30D158) else Color(0xFFFF453A))
                )
                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF8E9192),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickTriggerPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1F1F1F))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
