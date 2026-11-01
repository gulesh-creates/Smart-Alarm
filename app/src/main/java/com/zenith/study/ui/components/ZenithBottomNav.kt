package com.zenith.study.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenith.study.ui.theme.CardCharcoal
import com.zenith.study.ui.theme.NeonLime
import com.zenith.study.ui.theme.TextMuted

enum class ZenithTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TIMER("Timer", Icons.Default.HourglassTop),
    ALARMS("Alarms", Icons.Default.Alarm),
    PROGRESS("Progress", Icons.Default.BarChart),
    SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun ZenithBottomNav(
    currentTab: ZenithTab,
    onTabSelected: (ZenithTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(CardCharcoal),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ZenithTab.values().forEach { tab ->
            val isSelected = (currentTab == tab)
            val color = if (isSelected) NeonLime else TextMuted

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = tab.label,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

