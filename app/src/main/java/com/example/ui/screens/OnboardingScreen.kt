package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.model.IslandConfig
import com.example.model.IslandMode
import com.example.model.IslandNotification
import com.example.model.MediaTrack
import com.example.ui.components.DynamicIslandView
import com.example.ui.theme.ObsidianBackground

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var step by remember { mutableStateOf(0) }
    var isDemoExpanded by remember { mutableStateOf(false) }

    fun checkPermissions() {
        val hasPostNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        val flatNotif = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val hasNotifListener = flatNotif != null && flatNotif.contains(context.packageName)

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        if (step == 0 && hasPostNotif) {
            step = 1
        }
        if (step == 1 && hasOverlay) {
            step = 2
        }
        if (step == 2 && hasNotifListener) {
            step = 3
        }
        if (step == 3 && isIgnoringBattery) {
            onComplete()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        checkPermissions()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        checkPermissions()
        if (step == 0) {
            step = 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Live Interactive Demo Island
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                DynamicIslandView(
                    mode = IslandMode.NOTIFICATION,
                    isExpanded = isDemoExpanded,
                    config = IslandConfig(widthDp = 170, heightDp = 38),
                    mediaTrack = MediaTrack(title = "AURA Theme", artist = "Live Preview", isPlaying = false),
                    incomingCall = com.example.model.IncomingCall(),
                    notification = IslandNotification(
                        appName = "AURA",
                        sender = "Dynamic Island Live",
                        message = "Tap to expand the live island preview!"
                    ),
                    timerState = com.example.model.TimerState(),
                    chargingPercentage = 95,
                    customMessage = "AURA Live",
                    onIslandClick = { isDemoExpanded = !isDemoExpanded },
                    onTogglePlayback = {},
                    onCollapse = { isDemoExpanded = false },
                    applyPositionOffset = false
                )
            }

            Text(
                text = "Let's get AURA set up",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "AURA needs a few permissions to run the floating island reliably. This takes under a minute.",
                color = Color(0xFF8E9192),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(36.dp))

            when (step) {
                0 -> OnboardingStep(
                    title = "Notification access",
                    description = "Lets AURA show its own persistent status notification so Android doesn't kill the background service.",
                    buttonLabel = "Allow",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            step = 1
                        }
                    }
                )
                1 -> OnboardingStep(
                    title = "Display over other apps",
                    description = "This is what lets the island actually float on top of every other app.",
                    buttonLabel = "Open Settings",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            step = 2
                        }
                    }
                )
                2 -> OnboardingStep(
                    title = "Notification bridge access",
                    description = "Lets AURA surface your real notifications and now-playing media on the island.",
                    buttonLabel = "Open Settings",
                    onAction = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )
                3 -> OnboardingStep(
                    title = "Battery optimization",
                    description = "Without this, some phones will kill the island's background service after a while.",
                    buttonLabel = "Open Settings",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            onComplete()
                        }
                    }
                )
                else -> {
                    LaunchedEffect(Unit) {
                        onComplete()
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for now",
                    color = Color(0xFF8E9192),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onComplete() }
                )
                Text(
                    text = "${(step + 1).coerceAtMost(4)} of 4",
                    color = Color(0xFF8E9192),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    title: String,
    description: String,
    buttonLabel: String,
    onAction: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = description,
            color = Color(0xFF8E9192),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                text = buttonLabel,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}
