package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import com.example.ui.components.PerformanceModePicker
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IncomingCall
import com.example.model.IslandConfig
import com.example.model.IslandMode
import com.example.model.IslandNotification
import com.example.model.IslandShape
import com.example.model.MediaTrack
import com.example.model.TimerState
import com.example.ui.components.CrowdCanvas
import com.example.ui.components.DynamicIslandView
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.SurfaceContainer

enum class GeometryParam {
    SHAPE,
    WIDTH,
    HEIGHT,
    POSITION,
    RADIUS,
    COLOR_OPACITY,
    EFFECTS,
    PERFORMANCE,
    ALL
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeometryPreviewInspector(
    config: IslandConfig,
    onUpdateConfig: (IslandConfig) -> Unit,
    onBack: () -> Unit,
    onCustomizationPreviewActiveChange: (Boolean) -> Unit = {},
    initialParam: GeometryParam = GeometryParam.WIDTH,
    mediaTrack: MediaTrack = MediaTrack(),
    incomingCall: IncomingCall = IncomingCall(),
    notification: IslandNotification = IslandNotification(),
    timerState: TimerState = TimerState(),
    chargingPercentage: Int = 88,
    customMessage: String = "AURA Dynamic Island Preview",
    modifier: Modifier = Modifier
) {
    var selectedTool by remember { mutableStateOf<GeometryParam?>(initialParam) }
    var localConfig by remember(config) { mutableStateOf(config) }
    var customHexInput by remember(localConfig.backgroundColorHex) { mutableStateOf(localConfig.backgroundColorHex) }

    var dockHeightPx by remember { mutableStateOf(0) }
    val dockHeightDp = with(LocalDensity.current) { dockHeightPx.toDp() }

    DisposableEffect(Unit) {
        onCustomizationPreviewActiveChange(true)
        onDispose {
            onCustomizationPreviewActiveChange(false)
        }
    }

    // Debounce overlay-affecting config updates during continuous slider drags (~60fps update rate)
    LaunchedEffect(localConfig) {
        if (localConfig != config) {
            delay(16)
            onUpdateConfig(localConfig)
        }
    }

    val presetColors = remember {
        listOf(
            "#000000" to "Obsidian Black",
            "#0A2540" to "Deep Sapphire",
            "#1F0B2E" to "Midnight Violet",
            "#0A2E1C" to "Emerald Dark",
            "#33001A" to "Crimson Wine",
            "#222222" to "Charcoal Gray",
            "#003333" to "Teal Cyber",
            "#1A1A2E" to "Navy Velvet",
            "#FFFFFF" to "Pure Glass White"
        )
    }

    val presetColorsParsed = remember(presetColors) {
        presetColors.map { (hex, name) ->
            val swatchColor = try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                Color.Black
            }
            Triple(hex, name, swatchColor)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .testTag("geometry_preview_inspector")
    ) {
        // Moving Crowd Canvas on background
        CrowdCanvas(
            modifier = Modifier.fillMaxSize(),
            peepCount = 18,
            enabled = !localConfig.reduceMotion
        )

        // Live Dynamic Island Preview on Canvas
        DynamicIslandView(
            mode = IslandMode.COMPACT,
            isExpanded = false,
            config = localConfig,
            mediaTrack = mediaTrack,
            incomingCall = incomingCall,
            notification = notification,
            timerState = timerState,
            chargingPercentage = chargingPercentage,
            customMessage = customMessage,
            isCustomizationPreviewActive = true,
            onIslandClick = {},
            onTogglePlayback = {},
            onCollapse = {},
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        )

        // Bottom Sheet Customization Popup (Appears when a tool dock icon is tapped)
        AnimatedVisibility(
            visible = selectedTool != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val tool = selectedTool ?: GeometryParam.WIDTH

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dockHeightDp + 12.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfaceContainer.copy(alpha = 0.98f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header handle bar & close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (tool) {
                                GeometryParam.SHAPE -> "Customization: Island Base Shape"
                                GeometryParam.WIDTH -> "Customization: Island Width"
                                GeometryParam.HEIGHT -> "Customization: Island Height"
                                GeometryParam.POSITION -> "Customization: X & Y Position"
                                GeometryParam.RADIUS -> "Customization: Capsule Corner Radius"
                                GeometryParam.COLOR_OPACITY -> "Customization: Color & Transparency"
                                GeometryParam.EFFECTS -> "Customization: Effects & Haptics"
                                GeometryParam.PERFORMANCE -> "Customization: Performance"
                                GeometryParam.ALL -> "Customization: Full Inspector"
                            },
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.1f))
                                .clickable { selectedTool = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Bottom Sheet",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    when (tool) {
                        GeometryParam.SHAPE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Select Island Base Shape", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        IslandShape.DOT_EXPAND to "Dot & Expand",
                                        IslandShape.BROAD_DOCK to "Broad Dock",
                                        IslandShape.NOTCH to "Notch Silhouette"
                                    ).forEach { (shape, name) ->
                                        val isSelected = localConfig.islandShape == shape
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.08f))
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    localConfig = localConfig.copy(islandShape = shape)
                                                }
                                                .padding(vertical = 12.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = name,
                                                color = if (isSelected) Color.Black else Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        GeometryParam.WIDTH -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Compact Capsule Width", color = Color.White, fontSize = 13.sp)
                                    Text("${localConfig.widthDp} dp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = localConfig.widthDp.toFloat(),
                                    onValueChange = { localConfig = localConfig.copy(widthDp = it.toInt()) },
                                    valueRange = 100f..280f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color(0xFF444748)
                                    )
                                )
                            }
                        }

                        GeometryParam.HEIGHT -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Compact Capsule Height", color = Color.White, fontSize = 13.sp)
                                    Text("${localConfig.heightDp} dp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = localConfig.heightDp.toFloat(),
                                    onValueChange = { localConfig = localConfig.copy(heightDp = it.toInt()) },
                                    valueRange = 24f..64f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color(0xFF444748)
                                    )
                                )
                            }
                        }

                        GeometryParam.POSITION -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("X-Offset Position (Horizontal)", color = Color.White, fontSize = 13.sp)
                                        Text("${localConfig.offsetXDp} dp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = localConfig.offsetXDp.toFloat(),
                                        onValueChange = { localConfig = localConfig.copy(offsetXDp = it.toInt()) },
                                        valueRange = -160f..160f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color(0xFF444748)
                                        )
                                    )
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Y-Offset Position (Vertical)", color = Color.White, fontSize = 13.sp)
                                        Text("${localConfig.offsetYDp} dp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = localConfig.offsetYDp.toFloat(),
                                        onValueChange = { localConfig = localConfig.copy(offsetYDp = it.toInt()) },
                                        valueRange = 0f..220f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color(0xFF444748)
                                        )
                                    )
                                }
                            }
                        }

                        GeometryParam.RADIUS -> {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Corner Capsule Radius", color = Color.White, fontSize = 13.sp)
                                    Text("${localConfig.cornerRadiusDp} dp", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = localConfig.cornerRadiusDp.toFloat(),
                                    onValueChange = { localConfig = localConfig.copy(cornerRadiusDp = it.toInt()) },
                                    valueRange = 8f..32f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color.White,
                                        inactiveTrackColor = Color(0xFF444748)
                                    )
                                )
                            }
                        }

                        GeometryParam.COLOR_OPACITY -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Transparency Slider
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Transparency (Opacity)", color = Color.White, fontSize = 13.sp)
                                        Text("${(localConfig.backgroundAlpha * 100).toInt()}%", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Slider(
                                        value = localConfig.backgroundAlpha,
                                        onValueChange = { localConfig = localConfig.copy(backgroundAlpha = it) },
                                        valueRange = 0.1f..1.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color.White,
                                            inactiveTrackColor = Color(0xFF444748)
                                        )
                                    )
                                }

                                // Color Preset Swatches
                                Text("Preset Color Palette", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    presetColorsParsed.forEach { (hex, name, swatchColor) ->
                                        val isSelected = localConfig.backgroundColorHex.equals(hex, ignoreCase = true)

                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.2f),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    localConfig = localConfig.copy(backgroundColorHex = hex)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = name,
                                                    tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Custom Hex String Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customHexInput,
                                        onValueChange = {
                                            customHexInput = it
                                            if (it.startsWith("#") && (it.length == 7 || it.length == 9)) {
                                                localConfig = localConfig.copy(backgroundColorHex = it)
                                            }
                                        },
                                        label = { Text("Custom Color Hex", color = Color.Gray, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.White,
                                            unfocusedBorderColor = Color(0xFF444748),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF131313),
                                            unfocusedContainerColor = Color(0xFF131313)
                                        )
                                    )
                                }
                            }
                        }

                        GeometryParam.EFFECTS, GeometryParam.ALL -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Haptic Vibration", color = Color.White, fontSize = 13.sp)
                                        Text("Vibrate device on island interaction", color = Color(0xFF8E9192), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = localConfig.vibrationFeedback,
                                        onCheckedChange = { localConfig = localConfig.copy(vibrationFeedback = it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = Color.White
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Group Rapid Notifications", color = Color.White, fontSize = 13.sp)
                                        Text("Combine rapid alerts from same app (+count)", color = Color(0xFF8E9192), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = localConfig.groupRapidNotifications,
                                        onCheckedChange = { localConfig = localConfig.copy(groupRapidNotifications = it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = Color.White
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Respect Do Not Disturb", color = Color.White, fontSize = 13.sp)
                                        Text("Auto-pause island while phone is in DND/Focus", color = Color(0xFF8E9192), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = localConfig.respectDnd,
                                        onCheckedChange = { localConfig = localConfig.copy(respectDnd = it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = Color.White
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Reduce Motion", color = Color.White, fontSize = 13.sp)
                                        Text("Use instant snappy transitions without bouncy spring physics", color = Color(0xFF8E9192), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = localConfig.reduceMotion,
                                        onCheckedChange = { localConfig = localConfig.copy(reduceMotion = it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = Color.White
                                        )
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "Priority ranking when multiple events fire: Call > Notification > Charging > Timer > Media",
                                        color = Color(0xFF8E9192),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        GeometryParam.PERFORMANCE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Performance", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                PerformanceModePicker(
                                    currentMode = localConfig.performanceMode,
                                    onSelect = { mode -> localConfig = localConfig.copy(performanceMode = mode) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Classic Sleek Tools Dock Anchored at Bottom (Minimalist Icons)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .onGloballyPositioned { coordinates ->
                    val newHeight = coordinates.size.height
                    if (newHeight != dockHeightPx) {
                        dockHeightPx = newHeight
                    }
                }
                .clip(RoundedCornerShape(30.dp))
                .background(SurfaceContainer)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolDockButton(
                    icon = Icons.Default.Category,
                    label = "Shape",
                    isSelected = selectedTool == GeometryParam.SHAPE,
                    onClick = { selectedTool = GeometryParam.SHAPE }
                )
                ToolDockButton(
                    icon = Icons.Default.AspectRatio,
                    label = "Width",
                    isSelected = selectedTool == GeometryParam.WIDTH,
                    onClick = { selectedTool = GeometryParam.WIDTH }
                )
                ToolDockButton(
                    icon = Icons.Default.Height,
                    label = "Height",
                    isSelected = selectedTool == GeometryParam.HEIGHT,
                    onClick = { selectedTool = GeometryParam.HEIGHT }
                )
                ToolDockButton(
                    icon = Icons.Default.OpenWith,
                    label = "X / Y",
                    isSelected = selectedTool == GeometryParam.POSITION,
                    onClick = { selectedTool = GeometryParam.POSITION }
                )
                ToolDockButton(
                    icon = Icons.Default.RoundedCorner,
                    label = "Radius",
                    isSelected = selectedTool == GeometryParam.RADIUS,
                    onClick = { selectedTool = GeometryParam.RADIUS }
                )
                ToolDockButton(
                    icon = Icons.Default.ColorLens,
                    label = "Color",
                    isSelected = selectedTool == GeometryParam.COLOR_OPACITY,
                    onClick = { selectedTool = GeometryParam.COLOR_OPACITY }
                )
                ToolDockButton(
                    icon = Icons.Default.Tune,
                    label = "Effects",
                    isSelected = selectedTool == GeometryParam.EFFECTS,
                    onClick = { selectedTool = GeometryParam.EFFECTS }
                )
                ToolDockButton(
                    icon = Icons.Default.Speed,
                    label = "Performance",
                    isSelected = selectedTool == GeometryParam.PERFORMANCE,
                    onClick = { selectedTool = GeometryParam.PERFORMANCE }
                )
            }
        }
    }
}

@Composable
private fun ToolDockButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White else Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF8E9192),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
