package com.example.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

enum class IslandShape {
    DOT_EXPAND,   // Default pill / capsule notch collapsing to dot
    BROAD_DOCK,   // Wider, flatter bar-style resting state
    NOTCH         // Narrower, taller, rounded-bottom "notch" silhouette
}

enum class IslandMode {
    COMPACT,      // Default pill / capsule notch
    MEDIA,        // Music player with artwork & visualizer
    CALL,         // Incoming phone call banner
    NOTIFICATION, // Messaging or alert pop-up
    CHARGING,     // Battery charging status & percentage ring
    TIMER,        // Live countdown timer
    CUSTOM_TEXT   // Custom status or user message
}

enum class PerformanceMode {
    ADAPTIVE,        // auto-detect device tier, scale animation smoothness accordingly (default)
    HIGH_PERFORMANCE // always use the full premium spring animation, regardless of detected tier
}

enum class DockTab {
    HOME,
    MODULES,
    SETTINGS
}

/**
 * Describes the island's compact presentation level — matches Apple's 3-state model:
 *  IDLE     = Tiny collapsed dot (no activity)
 *  LIVE     = Compact pill showing leading icon + trailing info (activity running, not expanded)
 *  EXPANDED = Full widget (long press / tap triggered)
 */
enum class IslandCompactPresentation { IDLE, LIVE, EXPANDED }

@Immutable
data class IslandConfig(
    val widthDp: Int = 160,
    val heightDp: Int = 36,
    val offsetXDp: Int = 0,
    val offsetYDp: Int = 16,
    val cornerRadiusDp: Int = 24,
    val backgroundColorHex: String = "#000000",
    val backgroundAlpha: Float = 1.0f,
    val autoCollapseSeconds: Int = 5,
    val overlayServiceEnabled: Boolean = false,
    val vibrationFeedback: Boolean = true,
    val enabledModules: Set<String> = setOf("MEDIA", "CALL", "NOTIFICATION", "CHARGING", "TIMER", "WATER", "CUSTOM_TEXT"),
    val islandShape: IslandShape = IslandShape.DOT_EXPAND,
    val groupRapidNotifications: Boolean = true,
    val respectDnd: Boolean = true,
    val reduceMotion: Boolean = false,
    val performanceMode: PerformanceMode = PerformanceMode.ADAPTIVE
)

@Immutable
data class SavedProfile(
    val id: String,
    val name: String,
    val description: String,
    val config: IslandConfig
)

@Immutable
data class MediaTrack(
    val title: String = "Starry Night Waves",
    val artist: String = "AURA Ambient",
    val albumArtRes: Int? = null,
    val durationSeconds: Int = 210,
    val currentPositionSeconds: Int = 45,
    val isPlaying: Boolean = true
)

@Immutable
data class IncomingCall(
    val callerName: String = "Alex Morgan",
    val callerNumber: String = "+1 (555) 019-2834",
    val callType: String = "Incoming Audio Call"
)

@Immutable
data class IslandNotification(
    val id: Long = System.currentTimeMillis(),
    val appName: String = "AURA",
    val sender: String = "Elena Vance",
    val message: String = "Hey! Let's test the dynamic island custom pop-up.",
    val timeFormatted: String = "Just now",
    val appIcon: Drawable? = null,
    val packageName: String = "",
    val notificationKey: String = "",
    val canReply: Boolean = false,
    val isRead: Boolean = false,
    /** Dominant brand color extracted from the app icon for keyline tint. Null = no tint. */
    val tintColor: Int? = null
)

@Immutable
data class TimerState(
    val label: String = "Focus Timer",
    val totalSeconds: Int = 300,
    val remainingSeconds: Int = 184,
    val isRunning: Boolean = true
)
