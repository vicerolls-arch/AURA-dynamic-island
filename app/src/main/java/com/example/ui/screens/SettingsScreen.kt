package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import com.example.model.PerformanceMode
import com.example.ui.components.PerformanceModePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuraPreferences
import com.example.model.IslandConfig
import com.example.ui.theme.ObsidianBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: IslandConfig,
    onUpdateConfig: (IslandConfig) -> Unit,
    onOpenPreviewInspector: (GeometryParam) -> Unit,
    onOpenNotificationFilter: () -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val blockedPackages by AuraPreferences.getBlockedPackages(context).collectAsState(initial = emptySet())

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        )
    }

    val hasNotifAccess = remember {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        flat != null && flat.contains(context.packageName)
    }

    val isIgnoringBattery = remember {
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    val grantedCount = listOf(hasOverlayPermission, hasNotifAccess, isIgnoringBattery).count { it }

    var showPermissionsSheet by remember { mutableStateOf(false) }
    var showPerformanceSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val scrollState = rememberScrollState()

    val parsedBgColor = remember(config.backgroundColorHex) {
        try {
            Color(android.graphics.Color.parseColor(config.backgroundColorHex))
        } catch (e: Exception) {
            Color.Black
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("settings_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 110.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            // Section Header: Permissions
            Text(
                text = "SYSTEM PERMISSIONS",
                color = Color(0xFF8E9192),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )

            // Permissions Consolidation Row
            SettingsOptionRow(
                icon = Icons.Default.Security,
                label = "Permissions",
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$grantedCount/3 Active",
                            color = if (grantedCount == 3) Color(0xFF34C759) else Color(0xFFFF9500),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF8E9192),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                onClick = { showPermissionsSheet = true }
            )

            // Per-App Filter Row
            SettingsOptionRow(
                icon = Icons.Default.FilterList,
                label = "Per-App Filter Allow-List",
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (blockedPackages.isEmpty()) "All Apps Enabled" else "${blockedPackages.size} Muted",
                            color = Color(0xFF8E9192),
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF8E9192),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                onClick = {
                    showFilterSheet = true
                    onOpenNotificationFilter()
                }
            )

            // Section Header: Island Customization
            Text(
                text = "ISLAND CUSTOMIZATION",
                color = Color(0xFF8E9192),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 20.dp, bottom = 4.dp)
            )

            // Geometry Row
            SettingsOptionRow(
                icon = Icons.Default.AspectRatio,
                label = "Geometry & Dimensions",
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${config.widthDp}x${config.heightDp}dp",
                            color = Color(0xFF8E9192),
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF8E9192),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                onClick = { onOpenPreviewInspector(GeometryParam.WIDTH) }
            )

            // Color & Transparency Row
            SettingsOptionRow(
                icon = Icons.Default.ColorLens,
                label = "Color & Transparency",
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(parsedBgColor)
                        )
                        Text(
                            text = "${(config.backgroundAlpha * 100).toInt()}%",
                            color = Color(0xFF8E9192),
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF8E9192),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                onClick = { onOpenPreviewInspector(GeometryParam.COLOR_OPACITY) }
            )

            // Live Canvas Inspector Row
            SettingsOptionRow(
                icon = Icons.Default.Tune,
                label = "Live Canvas Inspector",
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF8E9192),
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { onOpenPreviewInspector(GeometryParam.ALL) }
            )

            // Performance Row
            SettingsOptionRow(
                icon = Icons.Default.Speed,
                label = "Performance",
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (config.performanceMode == PerformanceMode.ADAPTIVE) "Adaptive" else "High Performance",
                            color = Color(0xFF8E9192),
                            fontSize = 13.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF8E9192),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                onClick = { showPerformanceSheet = true }
            )
        }

        if (showPerformanceSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPerformanceSheet = false },
                containerColor = ObsidianBackground,
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text("Performance", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    PerformanceModePicker(
                        currentMode = config.performanceMode,
                        onSelect = { mode ->
                            onUpdateConfig(config.copy(performanceMode = mode))
                            showPerformanceSheet = false
                        }
                    )
                }
            }
        }

        // Modal Bottom Sheet for System Permissions
        if (showPermissionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPermissionsSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1C1C1E),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "System Permissions",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    PermissionSheetItem(
                        title = "Display Over Other Apps",
                        description = "Required to render floating system-wide dynamic island overlay",
                        isActive = hasOverlayPermission,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    )

                    PermissionSheetItem(
                        title = "Notification Bridge Access",
                        description = "Required to intercept and present system notifications live",
                        isActive = hasNotifAccess,
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )

                    PermissionSheetItem(
                        title = "Battery Optimization Exemption",
                        description = "Prevents Android OS from stopping the floating overlay service",
                        isActive = isIgnoringBattery,
                        onClick = {
                            if (!isIgnoringBattery) {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color(0xFF141416),
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        .fillMaxWidth()
                ) {
                    NotificationFilterScreen(
                        onBack = { showFilterSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsOptionRow(
    icon: ImageVector,
    label: String,
    trailingContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        trailingContent()
    }
}

@Composable
private fun PermissionSheetItem(
    title: String,
    description: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2C2C2E))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(0xFF8E9192),
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = if (isActive) "Active" else "Grant",
            color = if (isActive) Color(0xFF34C759) else Color(0xFFFF9500),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
