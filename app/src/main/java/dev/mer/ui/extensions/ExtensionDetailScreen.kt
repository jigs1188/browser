package dev.mer.ui.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.mer.ui.theme.Primary
import dev.mer.ui.theme.Secondary
import dev.mer.ui.theme.SurfaceCard
import dev.mer.ui.theme.SurfaceDark
import dev.mer.ui.theme.SurfaceDarkElevated
import dev.mer.ui.theme.TextMuted
import dev.mer.ui.theme.TextPrimary
import dev.mer.ui.theme.TextSecondary
import dev.mer.ui.theme.Warning
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExtensionDetailScreen(
    extensionId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExtensionDetailViewModel = hiltViewModel()
) {
    val extension by viewModel.getExtension(extensionId).collectAsState(initial = null)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = SurfaceDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        extension?.name ?: "Extension",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { paddingValues ->
        val ext = extension
        if (ext == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("Extension not found", color = TextMuted)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDarkElevated)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (ext.isEnabled) Primary.copy(alpha = 0.15f)
                                else TextMuted.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            tint = if (ext.isEnabled) Primary else TextMuted,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            ext.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            "v${ext.version}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }

                    Switch(
                        checked = ext.isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled(ext.id, !ext.isEnabled) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Primary,
                            checkedTrackColor = Primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = TextMuted.copy(alpha = 0.2f)
                        )
                    )
                }

                if (ext.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        ext.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permissions section
            val permissions = try {
                Json.decodeFromString<List<String>>(ext.permissions)
            } catch (_: Exception) { emptyList() }

            SectionCard(
                icon = Icons.Default.Security,
                title = "Permissions",
                isEmpty = permissions.isEmpty(),
                emptyText = "No permissions required"
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    permissions.forEach { perm ->
                        Chip(
                            text = perm,
                            color = when (perm) {
                                "ai" -> Secondary
                                "storage" -> Primary
                                else -> Warning
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // URL Patterns section
            val matchPatterns = try {
                Json.decodeFromString<List<String>>(ext.matchPatterns)
            } catch (_: Exception) { emptyList() }

            SectionCard(
                icon = Icons.Default.Language,
                title = "URL Patterns",
                isEmpty = matchPatterns.isEmpty(),
                emptyText = "No patterns"
            ) {
                matchPatterns.forEach { pattern ->
                    Text(
                        text = pattern,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scripts section
            val scripts = try {
                Json.decodeFromString<List<String>>(ext.scripts)
            } catch (_: Exception) { emptyList() }

            SectionCard(
                icon = Icons.Default.Code,
                title = "Scripts (${scripts.size})",
                isEmpty = scripts.isEmpty(),
                emptyText = "No scripts"
            ) {
                scripts.forEach { file ->
                    Text(
                        text = file,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Styles section
            val styles = try {
                Json.decodeFromString<List<String>>(ext.styles)
            } catch (_: Exception) { emptyList() }

            if (styles.isNotEmpty()) {
                SectionCard(
                    icon = Icons.Default.Style,
                    title = "Styles (${styles.size})",
                    isEmpty = false,
                    emptyText = ""
                ) {
                    styles.forEach { file ->
                        Text(
                            text = file,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Metadata section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDarkElevated)
                    .padding(16.dp)
            ) {
                Text(
                    "Details",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                MetadataRow("ID", ext.id)
                MetadataRow("Run at", ext.runAt)
                MetadataRow(
                    "Installed",
                    java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US)
                        .format(java.util.Date(ext.installedAt))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isEmpty: Boolean,
    emptyText: String,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDarkElevated)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (isEmpty) {
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted.copy(alpha = 0.6f)
            )
        } else {
            content()
        }
    }
}

@Composable
private fun Chip(
    text: String,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = TextSecondary
        )
    }
}
