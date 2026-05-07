package dev.mer.ui.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.mer.ui.theme.Primary
import dev.mer.ui.theme.SurfaceDarkElevated
import dev.mer.ui.theme.TextMuted
import dev.mer.ui.theme.TextPrimary
import dev.mer.ui.theme.TextSecondary
import dev.mer.ui.theme.UrlBarBackground

data class AiOverlayState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val response: String = "",
    val error: String? = null,
    val queryText: String = ""
)

@Composable
fun AiOverlay(
    state: AiOverlayState,
    onQueryChanged: (String) -> Unit,
    onAsk: (String) -> Unit,
    onSummarize: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(SurfaceDarkElevated)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick actions
            Row {
                TextButton(
                    onClick = onSummarize,
                    enabled = !state.isLoading
                ) {
                    Text(
                        "📝 Summarize page",
                        color = if (state.isLoading) TextMuted else Primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(UrlBarBackground)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = state.queryText,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(Primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (state.queryText.isNotBlank()) {
                                onAsk(state.queryText)
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (state.queryText.isEmpty()) {
                            Text(
                                "Ask about this page...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        innerTextField()
                    }
                )

                IconButton(
                    onClick = {
                        if (state.queryText.isNotBlank()) {
                            onAsk(state.queryText)
                            focusManager.clearFocus()
                        }
                    },
                    enabled = state.queryText.isNotBlank() && !state.isLoading,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Ask",
                        tint = if (state.queryText.isNotBlank() && !state.isLoading) Primary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Response area
            if (state.isLoading || state.response.isNotEmpty() || state.error != null) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp, max = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(UrlBarBackground.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    when {
                        state.isLoading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Thinking...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                        state.error != null -> {
                            Text(
                                state.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = dev.mer.ui.theme.Error
                            )
                        }
                        else -> {
                            Text(
                                state.response,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
}
