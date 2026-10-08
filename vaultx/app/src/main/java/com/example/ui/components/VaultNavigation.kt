package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.VaultStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.VaultNavTab

data class NavItem(
    val tab: VaultNavTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun VaultNavigationBar(
    currentTab: VaultNavTab,
    onTabSelected: (VaultNavTab) -> Unit,
    strings: VaultStrings
) {
    val items = listOf(
        NavItem(
            tab = VaultNavTab.DASHBOARD,
            label = strings.dashboard,
            selectedIcon = Icons.Filled.Dashboard,
            unselectedIcon = Icons.Outlined.Dashboard
        ),
        NavItem(
            tab = VaultNavTab.FILES,
            label = strings.files,
            selectedIcon = Icons.Filled.Folder,
            unselectedIcon = Icons.Outlined.Folder
        ),
        NavItem(
            tab = VaultNavTab.NOTES,
            label = strings.notes,
            selectedIcon = Icons.Filled.EditNote,
            unselectedIcon = Icons.Outlined.EditNote
        ),
        NavItem(
            tab = VaultNavTab.PASSWORDS,
            label = strings.passwords,
            selectedIcon = Icons.Filled.VpnKey,
            unselectedIcon = Icons.Outlined.VpnKey
        ),
        NavItem(
            tab = VaultNavTab.RECORDS,
            label = strings.records,
            selectedIcon = Icons.Filled.Badge,
            unselectedIcon = Icons.Outlined.Badge
        )
    )

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = VaultSurface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyberCyan,
                    selectedTextColor = CyberCyan,
                    indicatorColor = CyberCyanContainer,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}
