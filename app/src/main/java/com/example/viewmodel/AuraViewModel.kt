package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AlertHistoryEntity
import com.example.data.AuraDatabase
import com.example.data.AuraPreferences
import com.example.data.SavedProfileEntity
import com.example.model.DockTab
import com.example.model.IncomingCall
import com.example.model.IslandConfig
import com.example.model.IslandMode
import com.example.model.IslandShape
import com.example.model.IslandNotification
import com.example.model.MediaTrack
import com.example.model.SavedProfile
import com.example.model.TimerState
import com.example.service.AuraEventBus
import com.example.service.AuraNotificationListenerService
import com.example.service.AuraOverlayService
import com.example.service.AppForegroundTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        @Volatile var activeInstance: AuraViewModel? = null
    }

    private val db = AuraDatabase.getDatabase(application)
    private val alertDao = db.alertDao()
    private val savedProfileDao = db.savedProfileDao()

    val alertsHistory: StateFlow<List<AlertHistoryEntity>> = alertDao.getAllAlerts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeTab = MutableStateFlow(DockTab.HOME)
    val activeTab: StateFlow<DockTab> = _activeTab.asStateFlow()

    private val _islandMode = MutableStateFlow(IslandMode.COMPACT)
    val islandMode: StateFlow<IslandMode> = _islandMode.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private val _secondaryIslandMode = MutableStateFlow<IslandMode?>(null)
    val secondaryIslandMode: StateFlow<IslandMode?> = _secondaryIslandMode.asStateFlow()

    private val _config = MutableStateFlow(IslandConfig())
    val config: StateFlow<IslandConfig> = _config.asStateFlow()

    private var snoozedUntilMs: Long = 0L
    private val _isDismissedUntilRelaunch = MutableStateFlow(false)
    val isDismissedUntilRelaunch: StateFlow<Boolean> = _isDismissedUntilRelaunch.asStateFlow()

    private val _isMediaDetailExpanded = MutableStateFlow(false)
    val isMediaDetailExpanded: StateFlow<Boolean> = _isMediaDetailExpanded.asStateFlow()

    private val modePriority = mapOf(
        IslandMode.CALL to 0,
        IslandMode.NOTIFICATION to 1,
        IslandMode.CHARGING to 2,
        IslandMode.TIMER to 3,
        IslandMode.MEDIA to 4,
        IslandMode.CUSTOM_TEXT to 5,
        IslandMode.COMPACT to 99
    )

    fun isSnoozed(): Boolean = System.currentTimeMillis() < snoozedUntilMs

    fun snoozeIsland() {
        snoozedUntilMs = System.currentTimeMillis() + 5 * 60 * 1000L
        collapseToCompact()
        viewModelScope.launch {
            AuraPreferences.setSnoozedUntil(getApplication(), snoozedUntilMs)
        }
    }

    fun clearSnooze() {
        snoozedUntilMs = 0L
        viewModelScope.launch {
            AuraPreferences.setSnoozedUntil(getApplication(), 0L)
        }
    }

    fun dismissIslandUntilRelaunch() {
        _isDismissedUntilRelaunch.value = true
        collapseToCompact()
        viewModelScope.launch {
            AuraPreferences.setDismissedUntilRelaunch(getApplication(), true)
        }
    }

    private val defaultSavedProfiles = listOf(
        SavedProfile(
            id = "preset_oled_black",
            name = "OLED Stealth Black",
            description = "Classic dark notch with crisp borders",
            config = IslandConfig(
                backgroundColorHex = "#000000",
                backgroundAlpha = 1.0f
            )
        ),
        SavedProfile(
            id = "preset_neon_glass",
            name = "Neon Glassmorphism",
            description = "Translucent frosted glass with cyber cyan glow",
            config = IslandConfig(
                widthDp = 180,
                heightDp = 40,
                backgroundColorHex = "#0A2540",
                backgroundAlpha = 0.7f
            )
        ),
        SavedProfile(
            id = "preset_midnight_purple",
            name = "Midnight Purple",
            description = "Deep violet theme with subtle vibration",
            config = IslandConfig(
                widthDp = 170,
                heightDp = 38,
                backgroundColorHex = "#1F0B2E",
                backgroundAlpha = 0.85f
            )
        ),
        SavedProfile(
            id = "preset_minimal_compact",
            name = "Minimal Compact Pill",
            description = "Sleek low-profile notch for subtle notifications",
            config = IslandConfig(
                widthDp = 130,
                heightDp = 30,
                cornerRadiusDp = 18,
                backgroundColorHex = "#121212",
                backgroundAlpha = 0.9f
            )
        )
    )

    val savedProfiles: StateFlow<List<SavedProfile>> = savedProfileDao.getAllProfiles()
        .map { entities ->
            if (entities.isEmpty()) {
                defaultSavedProfiles
            } else {
                entities.map { entity ->
                    SavedProfile(
                        id = entity.id,
                        name = entity.name,
                        description = entity.description,
                        config = IslandConfig(
                            widthDp = entity.widthDp,
                            heightDp = entity.heightDp,
                            offsetXDp = entity.offsetXDp,
                            offsetYDp = entity.offsetYDp,
                            cornerRadiusDp = entity.cornerRadiusDp,
                            backgroundColorHex = entity.backgroundColorHex,
                            backgroundAlpha = entity.backgroundAlpha,
                            autoCollapseSeconds = entity.autoCollapseSeconds,
                            vibrationFeedback = entity.vibrationFeedback,
                            enabledModules = entity.enabledModules.split(",").filter { it.isNotBlank() }.toSet(),
                            islandShape = try {
                                IslandShape.valueOf(entity.islandShape)
                            } catch (e: Exception) {
                                IslandShape.DOT_EXPAND
                            }
                        )
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = defaultSavedProfiles
        )

    private val allModuleKeyToMode = mapOf(
        "MEDIA" to IslandMode.MEDIA,
        "CHARGING" to IslandMode.CHARGING,
        "WATER" to IslandMode.CUSTOM_TEXT,
        "CALL" to IslandMode.CALL,
        "NOTIFICATION" to IslandMode.NOTIFICATION,
        "TIMER" to IslandMode.TIMER,
        "CUSTOM_TEXT" to IslandMode.CUSTOM_TEXT
    )

    // Find generated album art resource if available
    private val albumArtDrawableRes = try {
        R.drawable.img_album_art_1784918015243
    } catch (e: Exception) {
        null
    }

    private val _mediaTrack = MutableStateFlow(
        MediaTrack(
            title = "",
            artist = "",
            albumArtRes = albumArtDrawableRes,
            durationSeconds = 0,
            currentPositionSeconds = 0,
            isPlaying = false
        )
    )
    val mediaTrack: StateFlow<MediaTrack> = _mediaTrack.asStateFlow()

    private val _incomingCall = MutableStateFlow(IncomingCall())
    val incomingCall: StateFlow<IncomingCall> = _incomingCall.asStateFlow()

    private val _currentNotification = MutableStateFlow(IslandNotification())
    val currentNotification: StateFlow<IslandNotification> = _currentNotification.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _chargingPercentage = MutableStateFlow(88)
    val chargingPercentage: StateFlow<Int> = _chargingPercentage.asStateFlow()

    private val _customMessage = MutableStateFlow("AURA Dynamic Island Active")
    val customMessage: StateFlow<String> = _customMessage.asStateFlow()

    private var autoCollapseJob: Job? = null
    private var timerJob: Job? = null

    init {
        activeInstance = this
        startTimerSimulation()

        viewModelScope.launch {
            if (savedProfileDao.getCount() == 0) {
                defaultSavedProfiles.forEach { profile ->
                    savedProfileDao.insertProfile(
                        SavedProfileEntity(
                            id = profile.id,
                            name = profile.name,
                            description = profile.description,
                            widthDp = profile.config.widthDp,
                            heightDp = profile.config.heightDp,
                            offsetXDp = profile.config.offsetXDp,
                            offsetYDp = profile.config.offsetYDp,
                            cornerRadiusDp = profile.config.cornerRadiusDp,
                            backgroundColorHex = profile.config.backgroundColorHex,
                            backgroundAlpha = profile.config.backgroundAlpha,
                            autoCollapseSeconds = profile.config.autoCollapseSeconds,
                            vibrationFeedback = profile.config.vibrationFeedback,
                            enabledModules = profile.config.enabledModules.joinToString(","),
                            islandShape = profile.config.islandShape.name
                        )
                    )
                }
            }
        }

        viewModelScope.launch {
            AuraPreferences.isOverlayEnabled(getApplication()).collect { enabled ->
                _config.value = _config.value.copy(overlayServiceEnabled = enabled)
            }
        }

        viewModelScope.launch {
            AuraPreferences.getSnoozedUntil(getApplication()).collect { timestamp ->
                snoozedUntilMs = timestamp
            }
        }

        viewModelScope.launch {
            AuraPreferences.getDismissedUntilRelaunch(getApplication()).collect { dismissed ->
                _isDismissedUntilRelaunch.value = dismissed
            }
        }

        viewModelScope.launch {
            AppForegroundTracker.isAppForeground.collect { isFg ->
                if (isFg) {
                    _isDismissedUntilRelaunch.value = false
                    AuraPreferences.setDismissedUntilRelaunch(getApplication(), false)
                }
            }
        }

        viewModelScope.launch {
            AuraEventBus.notifications.collect { notif ->
                triggerNotificationAlert(notif)
            }
        }

        viewModelScope.launch {
            AuraEventBus.calls.collect { call ->
                triggerCallAlert(call.callerName, call.callerNumber)
            }
        }

        viewModelScope.launch {
            AuraEventBus.media.collect { track ->
                applyMediaUpdate(track)
            }
        }
    }

    private val DEFAULT_CONTENT_UPDATE_BUDGET_MS = 1000L
    private val FREQUENT_UPDATE_BUDGET_MS = 250L
    private var lastMediaContentUpdateMs = 0L

    private fun applyMediaUpdate(track: MediaTrack) {
        val now = System.currentTimeMillis()
        val budget = if (_islandMode.value == IslandMode.TIMER) FREQUENT_UPDATE_BUDGET_MS else DEFAULT_CONTENT_UPDATE_BUDGET_MS
        if (now - lastMediaContentUpdateMs < budget) return
        lastMediaContentUpdateMs = now
        _mediaTrack.value = track
        updateSecondaryMode()
    }

    override fun onCleared() {
        super.onCleared()
        if (activeInstance === this) {
            activeInstance = null
        }
    }

    fun selectTab(tab: DockTab) {
        _activeTab.value = tab
    }

    fun toggleIslandExpand() {
        autoCollapseJob?.cancel()
        if (_islandMode.value == IslandMode.NOTIFICATION) {
            if (!_isNotificationDetailExpanded.value) {
                expandNotificationDetail()
            } else {
                collapseToCompact()
            }
            return
        }
        if (_mediaTrack.value.isPlaying) {
            if (_islandMode.value == IslandMode.MEDIA && _isExpanded.value) {
                if (_isMediaDetailExpanded.value) {
                    _isMediaDetailExpanded.value = false
                } else {
                    _isMediaDetailExpanded.value = true
                    scheduleMediaAutoMinimize()
                }
            } else {
                _islandMode.value = IslandMode.MEDIA
                _isExpanded.value = true
                _isMediaDetailExpanded.value = false
            }
        } else if (_islandMode.value == IslandMode.COMPACT) {
            _isExpanded.value = true
            autoCollapseJob = viewModelScope.launch {
                delay(3000L)
                collapseToCompact()
            }
        } else {
            _isExpanded.value = !_isExpanded.value
        }
    }

    private fun scheduleMediaAutoMinimize() {
        autoCollapseJob?.cancel()
        autoCollapseJob = viewModelScope.launch {
            delay(5000L)
            _isMediaDetailExpanded.value = false
        }
    }

    fun expandMediaDetail() {
        autoCollapseJob?.cancel()
        _isMediaDetailExpanded.value = true
        scheduleMediaAutoMinimize()
    }

    private val _isNotificationDetailExpanded = MutableStateFlow(false)
    val isNotificationDetailExpanded: StateFlow<Boolean> = _isNotificationDetailExpanded.asStateFlow()

    private val MIN_MS_BETWEEN_FULL_CYCLES = 1500L
    private var lastFullCollapseMs = 0L

    fun setIslandMode(mode: IslandMode, expand: Boolean = true) {
        val now = System.currentTimeMillis()
        if (mode != IslandMode.COMPACT && !_isExpanded.value && now - lastFullCollapseMs < MIN_MS_BETWEEN_FULL_CYCLES) {
            viewModelScope.launch {
                delay(MIN_MS_BETWEEN_FULL_CYCLES - (now - lastFullCollapseMs))
                setIslandModeInternal(mode, expand)
            }
            return
        }
        setIslandModeInternal(mode, expand)
    }

    private fun setIslandModeInternal(mode: IslandMode, expand: Boolean = true) {
        if (isSnoozed() || _isDismissedUntilRelaunch.value) return

        val isModuleEnabled = _config.value.enabledModules.let { enabled ->
            mode == IslandMode.COMPACT || enabled.any { key ->
                allModuleKeyToMode[key] == mode
            }
        }
        if (!isModuleEnabled) return // module is disabled — ignore trigger silently

        val currentPriority = modePriority[_islandMode.value] ?: 99
        val incomingPriority = modePriority[mode] ?: 99
        if (_isExpanded.value && incomingPriority > currentPriority && _islandMode.value != IslandMode.COMPACT) {
            return // a higher-priority event is already showing — don't let a lower one interrupt it
        }

        autoCollapseJob?.cancel()
        _islandMode.value = mode
        _isExpanded.value = expand
        updateSecondaryMode()

        if (expand && mode != IslandMode.COMPACT) {
            val autoSeconds = _config.value.autoCollapseSeconds
            if (mode == IslandMode.NOTIFICATION && !_isNotificationDetailExpanded.value) {
                // Short 4-second auto-collapse timer for mini peek state
                autoCollapseJob = viewModelScope.launch {
                    delay(4000L)
                    collapseToCompact()
                }
            } else if (autoSeconds > 0 && mode != IslandMode.MEDIA && mode != IslandMode.TIMER && mode != IslandMode.CALL) {
                autoCollapseJob = viewModelScope.launch {
                    delay(autoSeconds * 1000L)
                    collapseToCompact()
                }
            }
        }
    }

    fun expandNotificationDetail() {
        autoCollapseJob?.cancel()
        _isNotificationDetailExpanded.value = true
        val autoSeconds = _config.value.autoCollapseSeconds
        if (autoSeconds > 0) {
            autoCollapseJob = viewModelScope.launch {
                delay(autoSeconds * 1000L)
                collapseToCompact()
            }
        }
    }

    private val _isCustomizationPreviewActive = MutableStateFlow(false)
    val isCustomizationPreviewActive: StateFlow<Boolean> = _isCustomizationPreviewActive.asStateFlow()

    fun setCustomizationPreviewActive(active: Boolean) {
        _isCustomizationPreviewActive.value = active
        if (active) {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
                val serviceIntent = Intent(context, AuraOverlayService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            }
            _islandMode.value = IslandMode.COMPACT
            _isExpanded.value = false
        } else {
            collapseToCompact()
        }
    }

    fun collapseToCompact() {
        lastFullCollapseMs = System.currentTimeMillis()
        _isExpanded.value = false
        _islandMode.value = IslandMode.COMPACT
        updateSecondaryMode()
    }

    fun togglePlayback() {
        val listenerService = AuraNotificationListenerService.instance
        if (listenerService != null) {
            listenerService.togglePlayback()
        } else {
            val current = _mediaTrack.value
            _mediaTrack.value = current.copy(isPlaying = !current.isPlaying)
        }
    }

    fun skipNext() {
        AuraNotificationListenerService.instance?.skipNext()
    }

    fun skipPrevious() {
        AuraNotificationListenerService.instance?.skipPrevious()
    }

    fun sendReply(notificationKey: String, replyText: String) {
        AuraNotificationListenerService.instance?.sendReply(notificationKey, replyText)
    }

    fun silenceRinger() {
        try {
            val context = getApplication<Application>()
            val telecomManager = context.getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
            telecomManager?.silenceRinger()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun acceptCall() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val telecomManager = context.getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                telecomManager?.acceptRingingCall()
            } else {
                // Fallback: launch Phone app
                val intent = Intent(Intent.ACTION_DIAL).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        collapseToCompact()
    }

    fun declineCall() {
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val telecomManager = context.getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                telecomManager?.endCall()
            } else {
                silenceRinger()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        collapseToCompact()
    }

    fun pauseTimer() {
        val t = _timerState.value
        if (t.isRunning) {
            _timerState.value = t.copy(isRunning = false)
            updateSecondaryMode()
        }
    }

    fun resumeTimer() {
        val t = _timerState.value
        if (!t.isRunning && t.remainingSeconds > 0) {
            _timerState.value = t.copy(isRunning = true)
            updateSecondaryMode()
        }
    }

    fun resetTimer() {
        val t = _timerState.value
        _timerState.value = t.copy(remainingSeconds = t.totalSeconds, isRunning = false)
        updateSecondaryMode()
    }

    fun seekMedia(position: Int) {
        val current = _mediaTrack.value
        _mediaTrack.value = current.copy(currentPositionSeconds = position.coerceIn(0, current.durationSeconds))
    }

    fun triggerNotificationAlert(notif: IslandNotification) {
        _currentNotification.value = notif
        _isNotificationDetailExpanded.value = false
        setIslandMode(IslandMode.NOTIFICATION, expand = true)

        viewModelScope.launch {
            alertDao.insertAlert(
                AlertHistoryEntity(
                    appName = notif.appName,
                    sender = notif.sender,
                    message = notif.message,
                    triggerType = "NOTIFICATION"
                )
            )
        }
    }

    fun triggerNotificationAlert(
        appName: String = "WhatsApp",
        sender: String = "Elena Vance",
        message: String = "Dynamic Island live popup preview!"
    ) {
        val notif = IslandNotification(
            appName = appName,
            sender = sender,
            message = message
        )
        triggerNotificationAlert(notif)
    }

    fun openNotificationSource(notificationKey: String = _currentNotification.value.notificationKey) {
        AuraNotificationListenerService.instance?.openNotificationSource(notificationKey)
    }

    fun openMediaSource(context: Context) {
        AuraNotificationListenerService.instance?.openMediaSource(context)
    }

    fun openCallSource(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onIslandDoubleClick(context: Context) {
        if (_config.value.vibrationFeedback) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vibrator?.vibrate(30)
            }
        }

        when (_islandMode.value) {
            IslandMode.NOTIFICATION -> openNotificationSource(_currentNotification.value.notificationKey)
            IslandMode.MEDIA -> openMediaSource(context)
            IslandMode.CALL -> openCallSource(context)
            else -> {}
        }

        collapseToCompact()
    }

    fun triggerCallAlert(callerName: String = "Alex Morgan", callerNumber: String = "+1 (555) 019-2834") {
        _incomingCall.value = IncomingCall(callerName = callerName, callerNumber = callerNumber)
        setIslandMode(IslandMode.CALL, expand = true)

        viewModelScope.launch {
            alertDao.insertAlert(
                AlertHistoryEntity(
                    appName = "Phone",
                    sender = callerName,
                    message = "Incoming Call: $callerNumber",
                    triggerType = "CALL"
                )
            )
        }
    }

    fun triggerChargingAlert(percentage: Int = 88) {
        _chargingPercentage.value = percentage
        setIslandMode(IslandMode.CHARGING, expand = true)

        viewModelScope.launch {
            alertDao.insertAlert(
                AlertHistoryEntity(
                    appName = "System Battery",
                    sender = "Fast Charger",
                    message = "Battery charged to $percentage%",
                    triggerType = "CHARGING"
                )
            )
        }
    }

    fun triggerTimerAlert(label: String = "Focus Session", seconds: Int = 300) {
        _timerState.value = TimerState(label = label, totalSeconds = seconds, remainingSeconds = seconds, isRunning = true)
        setIslandMode(IslandMode.TIMER, expand = true)

        viewModelScope.launch {
            alertDao.insertAlert(
                AlertHistoryEntity(
                    appName = "Clock",
                    sender = label,
                    message = "Timer started for ${seconds / 60} min",
                    triggerType = "TIMER"
                )
            )
        }
    }

    fun triggerCustomMessage(msg: String) {
        _customMessage.value = msg
        setIslandMode(IslandMode.CUSTOM_TEXT, expand = true)

        viewModelScope.launch {
            alertDao.insertAlert(
                AlertHistoryEntity(
                    appName = "AURA Status",
                    sender = "User Custom",
                    message = msg,
                    triggerType = "CUSTOM_TEXT"
                )
            )
        }
    }

    fun updateConfig(
        widthDp: Int = _config.value.widthDp,
        heightDp: Int = _config.value.heightDp,
        offsetXDp: Int = _config.value.offsetXDp,
        offsetYDp: Int = _config.value.offsetYDp,
        cornerRadiusDp: Int = _config.value.cornerRadiusDp,
        backgroundColorHex: String = _config.value.backgroundColorHex,
        backgroundAlpha: Float = _config.value.backgroundAlpha,
        autoCollapseSeconds: Int = _config.value.autoCollapseSeconds,
        overlayServiceEnabled: Boolean = _config.value.overlayServiceEnabled,
        vibrationFeedback: Boolean = _config.value.vibrationFeedback,
        enabledModules: Set<String> = _config.value.enabledModules,
        islandShape: com.example.model.IslandShape = _config.value.islandShape
    ) {
        val previousOverlayState = _config.value.overlayServiceEnabled

        val newConfig = _config.value.copy(
            widthDp = widthDp,
            heightDp = heightDp,
            offsetXDp = offsetXDp,
            offsetYDp = offsetYDp,
            cornerRadiusDp = cornerRadiusDp,
            backgroundColorHex = backgroundColorHex,
            backgroundAlpha = backgroundAlpha,
            autoCollapseSeconds = autoCollapseSeconds,
            overlayServiceEnabled = overlayServiceEnabled,
            vibrationFeedback = vibrationFeedback,
            enabledModules = enabledModules,
            islandShape = islandShape
        )
        _config.value = newConfig
        AuraEventBus.updateConfig(newConfig)

        if (previousOverlayState != overlayServiceEnabled) {
            viewModelScope.launch {
                AuraPreferences.setOverlayEnabled(getApplication(), overlayServiceEnabled)
                val context = getApplication<Application>()
                val serviceIntent = Intent(context, AuraOverlayService::class.java)
                if (overlayServiceEnabled) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)) {
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                } else {
                    context.stopService(serviceIntent)
                }
            }
        }
    }

    fun toggleModule(moduleKey: String) {
        val currentModules = _config.value.enabledModules.toMutableSet()
        if (currentModules.contains(moduleKey)) {
            currentModules.remove(moduleKey)
        } else {
            currentModules.add(moduleKey)
        }
        val newConfig = _config.value.copy(enabledModules = currentModules)
        _config.value = newConfig
        AuraEventBus.updateConfig(newConfig)
    }

    fun applySavedProfile(profile: SavedProfile) {
        val current = _config.value
        val newConfig = current.copy(
            widthDp = profile.config.widthDp,
            heightDp = profile.config.heightDp,
            cornerRadiusDp = profile.config.cornerRadiusDp,
            backgroundColorHex = profile.config.backgroundColorHex,
            backgroundAlpha = profile.config.backgroundAlpha,
            islandShape = profile.config.islandShape
        )
        _config.value = newConfig
        AuraEventBus.updateConfig(newConfig)
    }

    fun saveCurrentProfile(name: String, description: String = "Custom user preset") {
        viewModelScope.launch {
            val id = "user_preset_${System.currentTimeMillis()}"
            val profileName = name.ifBlank { "Custom Preset" }
            savedProfileDao.insertProfile(
                SavedProfileEntity(
                    id = id,
                    name = profileName,
                    description = description,
                    widthDp = _config.value.widthDp,
                    heightDp = _config.value.heightDp,
                    offsetXDp = _config.value.offsetXDp,
                    offsetYDp = _config.value.offsetYDp,
                    cornerRadiusDp = _config.value.cornerRadiusDp,
                    backgroundColorHex = _config.value.backgroundColorHex,
                    backgroundAlpha = _config.value.backgroundAlpha,
                    autoCollapseSeconds = _config.value.autoCollapseSeconds,
                    vibrationFeedback = _config.value.vibrationFeedback,
                    enabledModules = _config.value.enabledModules.joinToString(","),
                    islandShape = _config.value.islandShape.name
                )
            )
        }
    }

    fun importProfileString(rawInput: String) {
        try {
            val trimmed = rawInput.trim()
            if (trimmed.startsWith("AURA_PRESET:")) {
                val parts = trimmed.split(":")
                val name = parts.getOrNull(1)?.ifBlank { "Imported Preset" } ?: "Imported Preset"
                val widthDp = parts.getOrNull(2)?.toIntOrNull() ?: 170
                val heightDp = parts.getOrNull(3)?.toIntOrNull() ?: 38
                val shapeName = parts.getOrNull(4) ?: "DOT_EXPAND"
                val colorHex = parts.getOrNull(5) ?: "#111111"

                val shape = try { IslandShape.valueOf(shapeName) } catch (e: Exception) { IslandShape.DOT_EXPAND }
                val importedProfile = SavedProfile(
                    id = "imported_${System.currentTimeMillis()}",
                    name = name,
                    description = "Imported from share code",
                    config = IslandConfig(
                        widthDp = widthDp,
                        heightDp = heightDp,
                        islandShape = shape,
                        backgroundColorHex = colorHex
                    )
                )
                restoreSavedProfile(importedProfile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveImportedProfile(profile: SavedProfile) {
        restoreSavedProfile(profile)
    }

    fun restoreSavedProfile(profile: SavedProfile) {
        viewModelScope.launch {
            savedProfileDao.insertProfile(
                SavedProfileEntity(
                    id = profile.id,
                    name = profile.name,
                    description = profile.description,
                    widthDp = profile.config.widthDp,
                    heightDp = profile.config.heightDp,
                    offsetXDp = profile.config.offsetXDp,
                    offsetYDp = profile.config.offsetYDp,
                    cornerRadiusDp = profile.config.cornerRadiusDp,
                    backgroundColorHex = profile.config.backgroundColorHex,
                    backgroundAlpha = profile.config.backgroundAlpha,
                    autoCollapseSeconds = profile.config.autoCollapseSeconds,
                    vibrationFeedback = profile.config.vibrationFeedback,
                    enabledModules = profile.config.enabledModules.joinToString(","),
                    islandShape = profile.config.islandShape.name
                )
            )
        }
    }

    fun deleteSavedProfile(id: String) {
        viewModelScope.launch {
            savedProfileDao.deleteById(id)
        }
    }

    fun deleteAlert(id: Long) {
        viewModelScope.launch {
            alertDao.deleteAlertById(id)
        }
    }

    fun restoreAlert(alert: AlertHistoryEntity) {
        viewModelScope.launch {
            alertDao.insertAlert(alert)
        }
    }

    fun clearAlertsHistory() {
        viewModelScope.launch {
            alertDao.clearAll()
        }
    }

    private fun startTimerSimulation() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val t = _timerState.value
                if (t.isRunning && t.remainingSeconds > 0) {
                    _timerState.value = t.copy(remainingSeconds = t.remainingSeconds - 1)
                    updateSecondaryMode()
                }
            }
        }
    }

    private fun updateSecondaryMode() {
        val primary = _islandMode.value
        val isMediaActive = _mediaTrack.value.isPlaying
        val isTimerActive = _timerState.value.isRunning && _timerState.value.remainingSeconds > 0

        _secondaryIslandMode.value = when {
            primary == IslandMode.CALL -> {
                if (isTimerActive) IslandMode.TIMER else if (isMediaActive) IslandMode.MEDIA else null
            }
            primary == IslandMode.NOTIFICATION -> {
                if (isTimerActive) IslandMode.TIMER else if (isMediaActive) IslandMode.MEDIA else null
            }
            primary == IslandMode.TIMER -> {
                if (isMediaActive) IslandMode.MEDIA else null
            }
            primary == IslandMode.MEDIA -> {
                if (isTimerActive) IslandMode.TIMER else null
            }
            primary == IslandMode.CHARGING -> {
                if (isTimerActive) IslandMode.TIMER else if (isMediaActive) IslandMode.MEDIA else null
            }
            primary == IslandMode.CUSTOM_TEXT -> {
                if (isTimerActive) IslandMode.TIMER else if (isMediaActive) IslandMode.MEDIA else null
            }
            else -> {
                null
            }
        }
    }

    fun swapPrimaryAndSecondary() {
        val oldPrimary = _islandMode.value
        val oldSecondary = _secondaryIslandMode.value ?: return

        _islandMode.value = oldSecondary
        _secondaryIslandMode.value = oldPrimary
        _isExpanded.value = false
    }
}
