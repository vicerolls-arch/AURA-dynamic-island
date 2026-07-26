package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.DockTab
import com.example.ui.theme.DockBackground
import com.example.ui.theme.DockBorder

@Composable
fun FloatingDock(
    selectedTab: DockTab,
    onTabSelected: (DockTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 24.dp, start = 32.dp, end = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(32.dp), spotColor = Color.Black)
                .clip(RoundedCornerShape(32.dp))
                .background(DockBackground)
                .border(1.dp, DockBorder, RoundedCornerShape(32.dp))
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("floating_dock_row"),
            horizontalArrangement = Arrangement.spacedBy(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockItem(
                selected = selectedTab == DockTab.HOME,
                activeIcon = Icons.Default.Home,
                inactiveIcon = Icons.Outlined.Home,
                contentDescription = "Home Tab",
                testTag = "dock_tab_home",
                onClick = { onTabSelected(DockTab.HOME) }
            )
            DockItem(
                selected = selectedTab == DockTab.MODULES,
                activeIcon = Icons.Default.Widgets,
                inactiveIcon = Icons.Outlined.Widgets,
                contentDescription = "Modules Tab",
                testTag = "dock_tab_modules",
                onClick = { onTabSelected(DockTab.MODULES) }
            )
            DockItem(
                selected = selectedTab == DockTab.SETTINGS,
                activeIcon = Icons.Default.Person,
                inactiveIcon = Icons.Outlined.Person,
                contentDescription = "Settings Tab",
                testTag = "dock_tab_settings",
                onClick = { onTabSelected(DockTab.SETTINGS) }
            )
        }
    }
}

@Composable
private fun DockItem(
    selected: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    val iconSize by animateDpAsState(
        targetValue = if (selected) 24.dp else 22.dp,
        animationSpec = spring(),
        label = "iconSize"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (selected) activeIcon else inactiveIcon,
            contentDescription = contentDescription,
            tint = if (selected) Color.White else Color(0xFF8E9192),
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (selected) Color.White else Color.Transparent)
        )
    }
}
