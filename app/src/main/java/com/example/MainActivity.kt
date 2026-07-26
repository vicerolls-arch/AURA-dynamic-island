package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AuraPreferences
import com.example.model.DockTab
import com.example.ui.components.FloatingDock
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.GeometryParam
import com.example.ui.screens.GeometryPreviewInspector
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModulesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AuraTheme
import com.example.ui.theme.ObsidianBackground
import com.example.viewmodel.AuraViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraTheme {
                AuraApp()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuraApp(viewModel: AuraViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var onboardingComplete by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        onboardingComplete = AuraPreferences.isOnboardingComplete(context).first()
    }

    when (onboardingComplete) {
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBackground)
            )
        }
        false -> {
            OnboardingScreen(
                onComplete = {
                    onboardingComplete = true
                    coroutineScope.launch {
                        AuraPreferences.setOnboardingComplete(context, true)
                    }
                }
            )
        }
        true -> {
            val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
            val islandMode by viewModel.islandMode.collectAsStateWithLifecycle()
            val isExpanded by viewModel.isExpanded.collectAsStateWithLifecycle()
            val config by viewModel.config.collectAsStateWithLifecycle()
            val mediaTrack by viewModel.mediaTrack.collectAsStateWithLifecycle()
            val incomingCall by viewModel.incomingCall.collectAsStateWithLifecycle()
            val currentNotification by viewModel.currentNotification.collectAsStateWithLifecycle()
            val timerState by viewModel.timerState.collectAsStateWithLifecycle()
            val chargingPercentage by viewModel.chargingPercentage.collectAsStateWithLifecycle()
            val customMessage by viewModel.customMessage.collectAsStateWithLifecycle()
            val isNotificationDetailExpanded by viewModel.isNotificationDetailExpanded.collectAsStateWithLifecycle()
            val isMediaDetailExpanded by viewModel.isMediaDetailExpanded.collectAsStateWithLifecycle()
            val alertsHistory by viewModel.alertsHistory.collectAsStateWithLifecycle()
            val savedProfiles by viewModel.savedProfiles.collectAsStateWithLifecycle()
            val secondaryIslandMode by viewModel.secondaryIslandMode.collectAsStateWithLifecycle()

            var activePreviewParam by remember { mutableStateOf<GeometryParam?>(null) }
            val hideDock = activePreviewParam != null

            val tabBackStack = remember { mutableStateListOf(DockTab.HOME) }

            val selectTabWithStack: (DockTab) -> Unit = { targetTab ->
                if (viewModel.activeTab.value != targetTab) {
                    viewModel.selectTab(targetTab)
                    if (tabBackStack.lastOrNull() != targetTab) {
                        tabBackStack.add(targetTab)
                    }
                }
            }

            BackHandler(enabled = activePreviewParam != null || tabBackStack.size > 1) {
                if (activePreviewParam != null) {
                    activePreviewParam = null
                } else if (tabBackStack.size > 1) {
                    tabBackStack.removeAt(tabBackStack.size - 1)
                    val previousTab = tabBackStack.last()
                    viewModel.selectTab(previousTab)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBackground)
            ) {
                AnimatedContent(
                    targetState = activePreviewParam,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.92f, animationSpec = tween(220)) + fadeIn(tween(220))) with
                        (scaleOut(targetScale = 0.92f, animationSpec = tween(180)) + fadeOut(tween(180)))
                    },
                    label = "inspectorTransition"
                ) { previewParam ->
                    if (previewParam != null) {
                        // Live Preview Inspector Page (blank canvas, live island, sliders at bottom)
                        GeometryPreviewInspector(
                            config = config,
                            onUpdateConfig = { newConfig ->
                                viewModel.updateConfig(
                                    widthDp = newConfig.widthDp,
                                    heightDp = newConfig.heightDp,
                                    offsetXDp = newConfig.offsetXDp,
                                    offsetYDp = newConfig.offsetYDp,
                                    cornerRadiusDp = newConfig.cornerRadiusDp,
                                    backgroundColorHex = newConfig.backgroundColorHex,
                                    backgroundAlpha = newConfig.backgroundAlpha,
                                    autoCollapseSeconds = newConfig.autoCollapseSeconds,
                                    overlayServiceEnabled = newConfig.overlayServiceEnabled,
                                    vibrationFeedback = newConfig.vibrationFeedback,
                                    enabledModules = newConfig.enabledModules,
                                    islandShape = newConfig.islandShape
                                )
                            },
                            onBack = { activePreviewParam = null },
                            onCustomizationPreviewActiveChange = { viewModel.setCustomizationPreviewActive(it) },
                            initialParam = previewParam,
                            mediaTrack = mediaTrack,
                            incomingCall = incomingCall,
                            notification = currentNotification,
                            timerState = timerState,
                            chargingPercentage = chargingPercentage,
                            customMessage = customMessage
                        )
                    } else {
                        // Active Tab Screen View
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                if (targetState == DockTab.SETTINGS || initialState == DockTab.SETTINGS) {
                                    (slideInVertically(animationSpec = tween(220), initialOffsetY = { it / 20 }) + fadeIn(tween(220))) with
                                    (slideOutVertically(animationSpec = tween(200), targetOffsetY = { it / 20 }) + fadeOut(tween(200)))
                                } else {
                                    fadeIn(tween(150)) with fadeOut(tween(120))
                                }
                            },
                            label = "tabTransition"
                        ) { tab ->
                            when (tab) {
                                DockTab.HOME -> {
                                    HomeScreen(
                                        alertsHistory = alertsHistory,
                                        islandMode = islandMode,
                                        isExpanded = isExpanded,
                                        config = config,
                                        mediaTrack = mediaTrack,
                                        incomingCall = incomingCall,
                                        notification = currentNotification,
                                        timerState = timerState,
                                        chargingPercentage = chargingPercentage,
                                        customMessage = customMessage,
                                        secondaryIslandMode = secondaryIslandMode,
                                        onSecondaryClick = { viewModel.swapPrimaryAndSecondary() },
                                        isNotificationDetailExpanded = isNotificationDetailExpanded,
                                        onExpandNotificationDetail = { viewModel.expandNotificationDetail() },
                                        isMediaDetailExpanded = isMediaDetailExpanded,
                                        onExpandMediaDetail = { viewModel.expandMediaDetail() },
                                        onIslandClick = { viewModel.toggleIslandExpand() },
                                        onIslandDoubleClick = { viewModel.onIslandDoubleClick(context) },
                                        onSwipeLeft = { viewModel.snoozeIsland() },
                                        onSwipeRight = { viewModel.dismissIslandUntilRelaunch() },
                                        onTogglePlayback = { viewModel.togglePlayback() },
                                        onSkipNext = { viewModel.skipNext() },
                                        onSkipPrevious = { viewModel.skipPrevious() },
                                        onSendReply = { key, text -> viewModel.sendReply(key, text) },
                                        onSilenceRinger = { viewModel.silenceRinger() },
                                        onAcceptCall = { viewModel.acceptCall() },
                                        onDeclineCall = { viewModel.declineCall() },
                                        onPauseTimer = { viewModel.pauseTimer() },
                                        onResumeTimer = { viewModel.resumeTimer() },
                                        onResetTimer = { viewModel.resetTimer() },
                                        onCollapse = { viewModel.collapseToCompact() },
                                        onQuickModuleClick = { mode ->
                                            viewModel.setIslandMode(mode, expand = true)
                                        },
                                        onClearAlertsHistory = { viewModel.clearAlertsHistory() },
                                        onDeleteAlert = { id -> viewModel.deleteAlert(id) },
                                        onLaunchOverlay = {
                                            val appCtx = viewModel.getApplication<android.app.Application>()
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(appCtx)) {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:${appCtx.packageName}")
                                                ).apply {
                                                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                appCtx.startActivity(intent)
                                            } else {
                                                viewModel.updateConfig(overlayServiceEnabled = true)
                                                val serviceIntent = android.content.Intent(appCtx, com.example.service.AuraOverlayService::class.java)
                                                androidx.core.content.ContextCompat.startForegroundService(appCtx, serviceIntent)
                                            }
                                        }
                                    )
                                }
                                DockTab.MODULES -> {
                                    ModulesScreen(
                                        config = config,
                                        savedProfiles = savedProfiles,
                                        onToggleModule = { moduleKey -> viewModel.toggleModule(moduleKey) },
                                        onTriggerMode = { mode ->
                                            viewModel.setIslandMode(mode, expand = true)
                                            selectTabWithStack(DockTab.HOME)
                                        },
                                        onOpenPersonalization = {
                                            activePreviewParam = GeometryParam.WIDTH
                                        },
                                        onApplyProfile = { profile -> viewModel.applySavedProfile(profile) },
                                        onSaveProfile = { name -> viewModel.saveCurrentProfile(name) },
                                        onDeleteProfile = { id -> viewModel.deleteSavedProfile(id) },
                                        onRestoreProfile = { profile -> viewModel.restoreSavedProfile(profile) },
                                        onImportProfile = { rawString -> viewModel.importProfileString(rawString) },
                                        onSendCustomText = { text ->
                                            viewModel.triggerCustomMessage(text)
                                            selectTabWithStack(DockTab.HOME)
                                        }
                                    )
                                }
                                DockTab.SETTINGS -> {
                                    SettingsScreen(
                                        config = config,
                                        onUpdateConfig = { newConfig ->
                                            viewModel.updateConfig(
                                                widthDp = newConfig.widthDp,
                                                heightDp = newConfig.heightDp,
                                                offsetXDp = newConfig.offsetXDp,
                                                offsetYDp = newConfig.offsetYDp,
                                                cornerRadiusDp = newConfig.cornerRadiusDp,
                                                backgroundColorHex = newConfig.backgroundColorHex,
                                                backgroundAlpha = newConfig.backgroundAlpha,
                                                autoCollapseSeconds = newConfig.autoCollapseSeconds,
                                                overlayServiceEnabled = newConfig.overlayServiceEnabled,
                                                vibrationFeedback = newConfig.vibrationFeedback,
                                                enabledModules = newConfig.enabledModules
                                            )
                                        },
                                        onOpenPreviewInspector = { param ->
                                            activePreviewParam = param
                                        },
                                        onBack = { selectTabWithStack(DockTab.HOME) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (!hideDock) {
                    // Floating Navigation Dock Anchored at Bottom
                    FloatingDock(
                        selectedTab = activeTab,
                        onTabSelected = { selectTabWithStack(it) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}
