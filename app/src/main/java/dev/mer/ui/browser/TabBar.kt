package dev.mer.ui.browser

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mer.ui.theme.Primary
import dev.mer.ui.theme.SurfaceCard
import dev.mer.ui.theme.SurfaceDark
import dev.mer.ui.theme.SurfaceDarkElevated
import dev.mer.ui.theme.TextMuted
import dev.mer.ui.theme.TextPrimary
import dev.mer.ui.theme.TextSecondary

@Composable
fun TabBar(
    tabs: List<TabState>,
    activeTabIndex: Int,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onAddTab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val isActive = index == activeTabIndex
            val bgColor by animateColorAsState(
                targetValue = if (isActive) SurfaceDarkElevated else SurfaceDark,
                label = "tabBg"
            )

            Row(
                modifier = Modifier
                    .height(34.dp)
                    .widthIn(min = 80.dp, max = 180.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(bgColor)
                    .clickable { onTabClick(index) }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tab.title.ifEmpty { "New Tab" },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) TextPrimary else TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (tabs.size > 1) {
                    IconButton(
                        onClick = { onTabClose(index) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close tab",
                            tint = TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // Add tab button
        IconButton(
            onClick = onAddTab,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "New tab",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
