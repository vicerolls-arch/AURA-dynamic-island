package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.example.data.AuraPreferences
import com.example.model.IslandNotification
import com.example.model.MediaTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuraNotificationListenerService : NotificationListenerService() {

    companion object {
        @Volatile
        var instance: AuraNotificationListenerService? = null
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var blockedPackages = emptySet<String>()
    private val recentNotificationCounts = mutableMapOf<String, Pair<Int, Long>>() // packageName -> (count, windowStartMs)

    private var activeMediaController: MediaController? = null
    private val pendingReplyActions = mutableMapOf<String, Notification.Action>()
    private val pendingContentIntents = mutableMapOf<String, PendingIntent>()

    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaTrack(metadata, activeMediaController?.playbackState)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaTrack(activeMediaController?.metadata, state)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        setupMediaSessionManager()

        serviceScope.launch {
            AuraPreferences.getBlockedPackages(applicationContext).collectLatest { set ->
                blockedPackages = set
            }
        }
    }

    private fun isDndActive(): Boolean {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        return nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun shouldGroupOrEmit(sbn: StatusBarNotification, notif: IslandNotification, groupingEnabled: Boolean): IslandNotification {
        if (!groupingEnabled) return notif
        val now = System.currentTimeMillis()
        val (prevCount, windowStart) = recentNotificationCounts[sbn.packageName] ?: (0 to now)
        return if (now - windowStart < 10_000L) {
            val newCount = prevCount + 1
            recentNotificationCounts[sbn.packageName] = newCount to windowStart
            notif.copy(message = "${notif.message}  (+$newCount)")
        } else {
            recentNotificationCounts[sbn.packageName] = 1 to now
            notif
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        if (sbn.packageName == packageName) return // ignore AURA's own notification
        if (blockedPackages.contains(sbn.packageName)) return // per-app filter allow-list

        val currentConfig = AuraEventBus.config.value
        if (currentConfig.respectDnd && isDndActive()) return // respect DND / Focus mode

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isNullOrBlank()) return

        val appLabel = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val appIcon = try {
            packageManager.getApplicationIcon(sbn.packageName)
        } catch (e: Exception) {
            null
        }

        val replyAction = sbn.notification.actions?.firstOrNull { action ->
            action.remoteInputs?.isNotEmpty() == true
        }

        if (replyAction != null) {
            pendingReplyActions[sbn.key] = replyAction
        }
        sbn.notification.contentIntent?.let {
            pendingContentIntents[sbn.key] = it
        }

        val tintColor = extractDominantColor(appIcon)

        val rawNotif = IslandNotification(
            id = sbn.postTime,
            appName = appLabel,
            sender = title,
            message = text,
            timeFormatted = "Just now",
            appIcon = appIcon,
            packageName = sbn.packageName,
            notificationKey = sbn.key,
            canReply = replyAction != null,
            tintColor = tintColor
        )

        val notification = shouldGroupOrEmit(sbn, rawNotif, currentConfig.groupRapidNotifications)
        AuraEventBus.tryPostNotification(notification)

        // Suppress the system notification popup — AURA is the sole notification display
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                snoozeNotification(sbn.key, Long.MAX_VALUE)
            } else {
                cancelNotification(sbn.key)
            }
        } catch (e: Exception) {
            // Fallback: some OEMs don't support snooze
            try { cancelNotification(sbn.key) } catch (_: Exception) {}
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        pendingReplyActions.remove(sbn.key)
        pendingContentIntents.remove(sbn.key)
    }

    fun openNotificationSource(notificationKey: String) {
        try {
            pendingContentIntents[notificationKey]?.send()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openMediaSource(context: Context) {
        val pkg = activeMediaController?.packageName ?: return
        try {
            context.packageManager.getLaunchIntentForPackage(pkg)?.let { intent ->
                context.startActivity(intent.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendReply(notificationKey: String, replyText: String) {
        val action = pendingReplyActions[notificationKey] ?: return
        val remoteInput = action.remoteInputs?.firstOrNull() ?: return
        val intent = Intent()
        val bundle = Bundle().apply {
            putCharSequence(remoteInput.resultKey, replyText)
        }
        android.app.RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)
        try {
            action.actionIntent.send(this, 0, intent)
        } catch (e: PendingIntent.CanceledException) {
            e.printStackTrace()
        }
    }

    fun togglePlayback() {
        val controller = activeMediaController ?: return
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipNext() {
        activeMediaController?.transportControls?.skipToNext()
    }

    fun skipPrevious() {
        activeMediaController?.transportControls?.skipToPrevious()
    }

    private fun setupMediaSessionManager() {
        try {
            val sessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
                ?: return
            val componentName = ComponentName(this, AuraNotificationListenerService::class.java)

            sessionManager.addOnActiveSessionsChangedListener({ controllers ->
                val controller = controllers?.firstOrNull()
                if (controller != activeMediaController) {
                    activeMediaController?.unregisterCallback(mediaCallback)
                    activeMediaController = controller
                    activeMediaController?.registerCallback(mediaCallback)
                    updateMediaTrack(controller?.metadata, controller?.playbackState)
                }
            }, componentName)

            val currentControllers = sessionManager.getActiveSessions(componentName)
            currentControllers.firstOrNull()?.let { controller ->
                activeMediaController = controller
                controller.registerCallback(mediaCallback)
                updateMediaTrack(controller.metadata, controller.playbackState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMediaTrack(metadata: MediaMetadata?, state: PlaybackState?) {
        if (metadata == null && state == null) return
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: ""
        val durationMs = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val durationSec = (durationMs / 1000).toInt()

        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val currentPositionMs = state?.position ?: 0L
        val currentPositionSec = (currentPositionMs / 1000).toInt()

        if (title.isNotBlank() || artist.isNotBlank()) {
            val track = MediaTrack(
                title = title.ifBlank { "Playing Media" },
                artist = artist.ifBlank { "Unknown Artist" },
                durationSeconds = if (durationSec > 0) durationSec else 180,
                currentPositionSeconds = currentPositionSec,
                isPlaying = isPlaying
            )
            AuraEventBus.tryPostMedia(track)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = null
        pendingReplyActions.clear()
        pendingContentIntents.clear()
    }

    private fun extractDominantColor(drawable: Drawable?): Int? {
        if (drawable == null) return null
        return try {
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> {
                    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
                    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1
                    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bmp
                }
            }
            val scaled = Bitmap.createScaledBitmap(bitmap, 1, 1, true)
            val color = scaled.getPixel(0, 0)
            if (scaled != bitmap) {
                scaled.recycle()
            }
            color
        } catch (e: Exception) {
            null
        }
    }
}
