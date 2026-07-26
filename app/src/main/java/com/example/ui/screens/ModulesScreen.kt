package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IslandConfig
import com.example.model.IslandMode
import com.example.model.SavedProfile
import com.example.ui.theme.ObsidianBackground

import android.content.Intent
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

data class ModuleInfo(
    val key: String,
    val title: String,
    val mode: IslandMode,
    val icon: ImageVector
)

@Composable
fun ModulesScreen(
    config: IslandConfig,
    savedProfiles: List<SavedProfile>,
    onToggleModule: (String) -> Unit,
    onTriggerMode: (IslandMode) -> Unit,
    onOpenPersonalization: () -> Unit,
    onApplyProfile: (SavedProfile) -> Unit,
    onSaveProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onRestoreProfile: (SavedProfile) -> Unit = {},
    onImportProfile: (String) -> Unit = {},
    onSendCustomText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }
    var importJsonInput by remember { mutableStateOf("") }

    val allModules = listOf(
        ModuleInfo("MEDIA", "Music & Audio", IslandMode.MEDIA, Icons.Default.MusicNote),
        ModuleInfo("CHARGING", "Battery Charging", IslandMode.CHARGING, Icons.Default.BatteryChargingFull),
        ModuleInfo("CALL", "Incoming Call", IslandMode.CALL, Icons.Default.PhoneInTalk),
        ModuleInfo("NOTIFICATION", "App Notifications", IslandMode.NOTIFICATION, Icons.Default.Message),
        ModuleInfo("TIMER", "Countdown Timer", IslandMode.TIMER, Icons.Default.Timer),
        ModuleInfo("CUSTOM_TEXT", "Custom Status Text", IslandMode.CUSTOM_TEXT, Icons.Default.TextFields),
        ModuleInfo("WATER", "Hydration Reminder", IslandMode.CUSTOM_TEXT, Icons.Default.WaterDrop)
    )

    val filteredModules = remember(searchQuery) {
        if (searchQuery.isBlank()) allModules
        else allModules.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .testTag("modules_screen_container")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, bottom = 110.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header
            Text(
                text = "Modules",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search modules...", color = Color(0xFF8E9192), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF333333),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF141414),
                    unfocusedContainerColor = Color(0xFF141414)
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Module Items List
            filteredModules.forEach { module ->
                val isEnabled = config.enabledModules.contains(module.key)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = module.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isEnabled) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                                .clickable(enabled = isEnabled) { onTriggerMode(module.mode) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Test",
                                tint = if (isEnabled) Color.White else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { onToggleModule(module.key) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Color.White,
                                uncheckedTrackColor = Color(0xFF333333)
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }

            // Saved Profiles Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SAVED PRESETS",
                    color = Color(0xFF8E9192),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                            .clickable { showImportDialog = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Import",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .clickable { showSaveDialog = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+ Save Preset",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

            // Saved Profiles Flat List
            savedProfiles.forEach { profile ->
                key(profile.id) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = profile.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            val shapeLabel = when (profile.config.islandShape) {
                                com.example.model.IslandShape.DOT_EXPAND -> "Dot Pill"
                                com.example.model.IslandShape.BROAD_DOCK -> "Broad Dock"
                                com.example.model.IslandShape.NOTCH -> "Notch"
                            }
                            Text(
                                text = "Shape: $shapeLabel • ${profile.config.widthDp}x${profile.config.heightDp}dp",
                                color = Color(0xFF8E9192),
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { onApplyProfile(profile) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Apply", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .clickable {
                                        val exportText = "AURA_PRESET:${profile.name}:${profile.config.widthDp}:${profile.config.heightDp}:${profile.config.islandShape.name}:${profile.config.backgroundColorHex}"
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "AURA Preset - ${profile.name}")
                                            putExtra(Intent.EXTRA_TEXT, exportText)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export Preset"))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export Preset",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            if (!profile.id.startsWith("preset_")) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.08f))
                                        .clickable {
                                            onDeleteProfile(profile.id)
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Preset '${profile.name}' deleted",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    onRestoreProfile(profile)
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 110.dp)
        )
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Preset", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = presetNameInput,
                    onValueChange = { presetNameInput = it },
                    placeholder = { Text("Preset name", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0xFF444748),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable {
                            if (presetNameInput.isNotBlank()) {
                                onSaveProfile(presetNameInput)
                                presetNameInput = ""
                                showSaveDialog = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Text(
                    text = "Cancel",
                    color = Color.Gray,
                    modifier = Modifier
                        .clickable { showSaveDialog = false }
                        .padding(8.dp)
                )
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Preset", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Paste an exported AURA preset string below:",
                        color = Color(0xFF8E9192),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("AURA_PRESET:Name:Width:Height...", color = Color.Gray) },
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0xFF444748),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable {
                            if (importJsonInput.isNotBlank()) {
                                onImportProfile(importJsonInput)
                                importJsonInput = ""
                                showImportDialog = false
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Import", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Text(
                    text = "Cancel",
                    color = Color.Gray,
                    modifier = Modifier
                        .clickable { showImportDialog = false }
                        .padding(8.dp)
                )
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}
