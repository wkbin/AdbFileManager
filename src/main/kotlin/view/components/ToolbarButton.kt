package view.components

import io.github.composefluent.FluentTheme
import io.github.composefluent.component.Text
import io.github.composefluent.component.Icon
import view.fluent.*

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 工具栏按钮
 */
@Composable
fun ToolbarButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    tint: Color = FluentTheme.colors.text.accent.primary,
    enabled: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    io.github.composefluent.component.SubtleButton(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 2.dp),
        disabled = !enabled,
        iconOnly = compact
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (compact) text else null,
            tint = if (enabled) tint else FluentTheme.colors.text.text.disabled,
            modifier = Modifier.size(18.dp)
        )
        if (!compact) {
            Text(text = text, style = FluentTheme.typography.caption)
        }
    }
}
