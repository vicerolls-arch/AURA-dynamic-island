package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.model.IncomingCall
import com.example.model.IslandConfig
import com.example.model.IslandMode
import com.example.model.IslandNotification
import com.example.model.MediaTrack
import com.example.model.PerformanceMode
import com.example.model.TimerState
import com.example.util.DeviceCapability
import com.example.util.DeviceTier
import com.example.ui.components.DynamicIslandView
import com.example.ui.theme.AuraTheme
import com.example.viewmodel.AuraViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AuraOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val overlayLifecycleOwner = ServiceLifecycleOwner()

    private var islandMode by mutableStateOf(IslandMode.COMPACT)
    private var isExpanded by mutableStateOf(false)
    private var config by mutableStateOf(IslandConfig())
    private var mediaTrack by mutableStateOf(MediaTrack())
    private var incomingCall by mutableStateOf(IncomingCall())
    private var notification by mutableStateOf(IslandNotification())
    private var timerState by mutableStateOf(TimerState())
    private var chargingPercentage by mutableIntStateOf(88)
    private var customMessage by mutableStateOf("AURA Dynamic Banner")

    private var batteryReceiver: BroadcastReceiver? = null
    private var wasCharging = false
    private var autoCollapseJob: Job? = null
    private var lastKnownConfig = IslandConfig()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlayLifecycleOwner.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (windowManager == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                stopSelf()
                return START_NOT_STICKY
            }
            createNotificationChannel()
            startForegroundServiceNotification()
            setupOverlayWindow()
            setupReceiversAndListeners()
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, AuraOverlayService::class.java).apply {
            setPackage(packageName)
        }
        val pendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "aura_island_channel",
                "AURA Dynamic Island Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, "aura_island_channel")
            .setContentTitle("AURA Dynamic Island")
            .setContentText("Dynamic island floating overlay active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getStatusBarHeightPx(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
    }

    private fun getCutoutCenterXPx(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null

        val windowMetrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics
        } else null

        val cutout = if (windowMetrics != null) {
            windowMetrics.windowInsets.displayCutout
        } else {
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.let { display ->
                overlayView?.rootWindowInsets?.displayCutout
            }
        } ?: return null

        val boundingRects = cutout.boundingRects
        if (boundingRects.isEmpty()) return null

        val topCutout = boundingRects.firstOrNull { it.top < 200 } ?: boundingRects.first()
        return topCutout.centerX()
    }

    private fun setupOverlayWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val paramsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val density = resources.displayMetrics.density
        val estimatedIslandWidthPx = (config.widthDp * density).toInt()
        val statusBarHeight = getStatusBarHeightPx()
        val cutoutCenterX = getCutoutCenterXPx()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            paramsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            if (cutoutCenterX != null) {
                gravity = Gravity.TOP or Gravity.START
                x = cutoutCenterX - (estimatedIslandWidthPx / 2) + (config.offsetXDp * density).toInt()
            } else {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                x = (config.offsetXDp * density).toInt()
            }
            y = statusBarHeight + (config.offsetYDp * density).toInt()

            val shouldRequestHighRefreshRate = config.performanceMode == PerformanceMode.HIGH_PERFORMANCE ||
                DeviceCapability.detectTier(this@AuraOverlayService) == DeviceTier.HIGH

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && shouldRequestHighRefreshRate) {
                val refreshRate = DeviceCapability.detectRefreshRateHz(this@AuraOverlayService)
                if (refreshRate > 60f) {
                    preferredRefreshRate = refreshRate
                }
            }
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)

            setContent {
                val vm = AuraViewModel.activeInstance

                if (vm == null) {
                    AuraTheme {
                        DynamicIslandView(
                            mode = islandMode,
                            isExpanded = isExpanded,
                            config = lastKnownConfig,
                            mediaTrack = mediaTrack,
                            incomingCall = incomingCall,
                            notification = notification,
                            timerState = timerState,
                            chargingPercentage = chargingPercentage,
                            customMessage = customMessage,
                            onIslandClick = {
                                if (islandMode == IslandMode.COMPACT) {
                                    triggerMode(IslandMode.MEDIA, expand = true)
                                } else {
                                    isExpanded = !isExpanded
                                }
                            },
                            onTogglePlayback = {
                                mediaTrack = mediaTrack.copy(isPlaying = !mediaTrack.isPlaying)
                            },
                            onCollapse = {
                                isExpanded = false
                                islandMode = IslandMode.COMPACT
                            },
                            applyPositionOffset = false
                        )
                    }
                } else {
                    val configState by vm.config.collectAsState()
                    val islandModeState by vm.islandMode.collectAsState()
                    val isExpandedState by vm.isExpanded.collectAsState()
                    val mediaTrackState by vm.mediaTrack.collectAsState()
                    val incomingCallState by vm.incomingCall.collectAsState()
                    val notificationState by vm.currentNotification.collectAsState()
                    val timerStateState by vm.timerState.collectAsState()
                    val chargingPercentageState by vm.chargingPercentage.collectAsState()
                    val customMessageState by vm.customMessage.collectAsState()
                    val isCustomizationPreviewActiveState by vm.isCustomizationPreviewActive.collectAsState()
                    val isNotificationDetailExpandedState by vm.isNotificationDetailExpanded.collectAsState()
                    val isMediaDetailExpandedState by vm.isMediaDetailExpanded.collectAsState()
                    val secondaryIslandModeState by vm.secondaryIslandMode.collectAsState()

                    lastKnownConfig = configState

                    LaunchedEffect(configState.offsetXDp, configState.offsetYDp, configState.widthDp, configState.heightDp, configState.backgroundColorHex, configState.backgroundAlpha) {
                        updateWindowPosition(configState.offsetXDp, configState.offsetYDp)
                    }

                    LaunchedEffect(isNotificationDetailExpandedState) {
                        updateWindowFocusable(isNotificationDetailExpandedState)
                    }

                    AuraTheme {
                        DynamicIslandView(
                            mode = islandModeState,
                            isExpanded = isExpandedState,
                            config = configState,
                            mediaTrack = mediaTrackState,
                            incomingCall = incomingCallState,
                            notification = notificationState,
                            timerState = timerStateState,
                            chargingPercentage = chargingPercentageState,
                            customMessage = customMessageState,
                            isCustomizationPreviewActive = isCustomizationPreviewActiveState,
                            isNotificationDetailExpanded = isNotificationDetailExpandedState,
                            onExpandNotificationDetail = { vm.expandNotificationDetail() },
                            isMediaDetailExpanded = isMediaDetailExpandedState,
                            onExpandMediaDetail = { vm.expandMediaDetail() },
                            onIslandClick = { vm.toggleIslandExpand() },
                            onIslandDoubleClick = { vm.onIslandDoubleClick(this@AuraOverlayService) },
                            onSwipeLeft = { vm.snoozeIsland() },
                            onSwipeRight = { vm.dismissIslandUntilRelaunch() },
                            onTogglePlayback = { vm.togglePlayback() },
                            onSkipNext = { vm.skipNext() },
                            onSkipPrevious = { vm.skipPrevious() },
                            onSendReply = { key, text -> vm.sendReply(key, text) },
                            onSilenceRinger = { vm.silenceRinger() },
                            onAcceptCall = { vm.acceptCall() },
                            onDeclineCall = { vm.declineCall() },
                            onPauseTimer = { vm.pauseTimer() },
                            onResumeTimer = { vm.resumeTimer() },
                            onResetTimer = { vm.resetTimer() },
                            onCollapse = { vm.collapseToCompact() },
                            secondaryMode = secondaryIslandModeState,
                            onSecondaryClick = { vm.swapPrimaryAndSecondary() },
                            applyPositionOffset = false
                        )
                    }
                }
            }
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            overlayView = null
            windowManager = null
            stopSelf()
        }
    }

    private fun updateWindowPosition(offsetXDp: Int, offsetYDp: Int) {
        val density = resources.displayMetrics.density
        val statusBarHeight = getStatusBarHeightPx()
        val params = overlayView?.layoutParams as? WindowManager.LayoutParams ?: return
        val estimatedIslandWidthPx = (config.widthDp * density).toInt()
        val cutoutCenterX = getCutoutCenterXPx()

        if (cutoutCenterX != null) {
            params.gravity = Gravity.TOP or Gravity.START
            params.x = cutoutCenterX - (estimatedIslandWidthPx / 2) + (offsetXDp * density).toInt()
        } else {
            params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            params.x = (offsetXDp * density).toInt()
        }
        params.y = statusBarHeight + (offsetYDp * density).toInt()

        try {
            windowManager?.updateViewLayout(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateWindowFocusable(focusable: Boolean) {
        val params = overlayView?.layoutParams as? WindowManager.LayoutParams ?: return
        val currentFlags = params.flags
        val newFlags = if (focusable) {
            currentFlags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        } else {
            currentFlags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        }
        if (newFlags != currentFlags) {
            params.flags = newFlags
            try {
                windowManager?.updateViewLayout(overlayView, params)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupReceiversAndListeners() {
        registerBatteryReceiver()
        setupTelephonyListener()
        listenToEventBus()
    }

    private fun listenToEventBus() {
        serviceScope.launch {
            AuraEventBus.config.collect { newConfig ->
                config = newConfig
                lastKnownConfig = newConfig
                updateWindowPosition(newConfig.offsetXDp, newConfig.offsetYDp)
            }
        }

        serviceScope.launch {
            AuraEventBus.notifications.collect { notif ->
                if (config.enabledModules.contains("NOTIFICATION")) {
                    notification = notif
                    triggerMode(IslandMode.NOTIFICATION, expand = true)
                }
            }
        }

        serviceScope.launch {
            AuraEventBus.calls.collect { call ->
                if (config.enabledModules.contains("CALL")) {
                    incomingCall = call
                    triggerMode(IslandMode.CALL, expand = true)
                }
            }
        }

        serviceScope.launch {
            AuraEventBus.media.collect { track ->
                if (config.enabledModules.contains("MEDIA")) {
                    mediaTrack = track
                    if (islandMode == IslandMode.COMPACT || islandMode == IslandMode.MEDIA) {
                        islandMode = IslandMode.MEDIA
                    }
                }
            }
        }
    }

    private fun registerBatteryReceiver() {
        if (batteryReceiver != null) return
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 88

                    val isCharging = plugged != 0
                    chargingPercentage = pct

                    if (isCharging && !wasCharging && config.enabledModules.contains("CHARGING")) {
                        triggerMode(IslandMode.CHARGING, expand = true)
                    }
                    wasCharging = isCharging
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    private fun setupTelephonyListener() {
        try {
            val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallState(state, null)
                    }
                }
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state, phoneNumber)
                    }
                }
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleCallState(state: Int, number: String?) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            val caller = if (!number.isNullOrBlank()) number else "Incoming Call"
            incomingCall = IncomingCall(callerName = caller, callerNumber = number ?: "Unknown")
            triggerMode(IslandMode.CALL, expand = true)
        }
    }

    private fun triggerMode(mode: IslandMode, expand: Boolean) {
        autoCollapseJob?.cancel()
        islandMode = mode
        isExpanded = expand

        if (expand && mode != IslandMode.COMPACT) {
            val autoSeconds = config.autoCollapseSeconds
            if (autoSeconds > 0 && mode != IslandMode.MEDIA && mode != IslandMode.TIMER) {
                autoCollapseJob = serviceScope.launch {
                    delay(autoSeconds * 1000L)
                    isExpanded = false
                    islandMode = IslandMode.COMPACT
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayLifecycleOwner.onDestroy()
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        batteryReceiver = null

        if (overlayView != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
