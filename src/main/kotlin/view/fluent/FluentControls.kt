package view.fluent

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.composefluent.FluentTheme
import io.github.composefluent.ProvideTextStyle
import io.github.composefluent.background.Layer
import io.github.composefluent.component.*
import io.github.composefluent.scheme.VisualStateScheme

// Shared application layouts keep existing action/state contracts while Fluent owns controls.
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = FluentTheme.colors.background.solid.tertiary,
    content: @Composable () -> Unit
) {
    Layer(modifier = modifier, shape = shape, color = color, border = null,
        content = content)
}

object CardDefaults {
    @Composable
    fun cardColors(containerColor: Color = FluentTheme.colors.background.card.default) = containerColor
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = FluentTheme.shapes.overlay,
    colors: Color = FluentTheme.colors.background.card.default,
    border: BorderStroke? = BorderStroke(1.dp, FluentTheme.colors.stroke.card.default),
    content: @Composable ColumnScope.() -> Unit
) {
    Layer(modifier = modifier, shape = shape, color = colors, border = border) { Column(content = content) }
}

object ButtonDefaults {
    @Composable
    fun buttonColors(
        containerColor: Color = FluentTheme.colors.fillAccent.default,
        contentColor: Color = FluentTheme.colors.text.onAccent.primary
    ): VisualStateScheme<ButtonColor> = io.github.composefluent.component.ButtonDefaults.accentButtonColors(
        default = ButtonColor(containerColor, contentColor, SolidColor(Color.Transparent)),
        hovered = ButtonColor(containerColor.copy(alpha = 0.9f), contentColor, SolidColor(Color.Transparent)),
        pressed = ButtonColor(containerColor.copy(alpha = 0.8f), contentColor, SolidColor(Color.Transparent))
    )

    @Composable
    fun textButtonColors(contentColor: Color = FluentTheme.colors.text.text.primary) =
        io.github.composefluent.component.ButtonDefaults.subtleButtonColors(
            default = ButtonColor(Color.Transparent, contentColor, SolidColor(Color.Transparent))
        )
}

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: VisualStateScheme<ButtonColor> = io.github.composefluent.component.ButtonDefaults.accentButtonColors(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable RowScope.() -> Unit
) {
    AccentButton(onClick, modifier, disabled = !enabled, buttonColors = colors) {
        Row(Modifier.padding(contentPadding), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: VisualStateScheme<ButtonColor> = io.github.composefluent.component.ButtonDefaults.subtleButtonColors(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable RowScope.() -> Unit
) {
    SubtleButton(onClick, modifier, disabled = !enabled, buttonColors = colors) {
        Row(Modifier.padding(contentPadding), verticalAlignment = Alignment.CenterVertically, content = content)
    }
}

@Composable
fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    io.github.composefluent.component.Button(onClick, modifier, disabled = !enabled, content = content)
}

@Composable
fun FilledTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    io.github.composefluent.component.Button(onClick, modifier, disabled = !enabled, content = content)
}

@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable () -> Unit) {
    SubtleButton(onClick, modifier, disabled = !enabled, iconOnly = true) { content() }
}

@Composable
fun Checkbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    CheckBox(checked = checked, modifier = modifier, onCheckStateChange = onCheckedChange)
}

@Composable
fun Switch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switcher(checked = checked, onCheckStateChange = onCheckedChange)
}

@Composable
fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    ToggleButton(checked = selected, onCheckedChanged = { onClick() }) { label() }
}

@Composable
fun HorizontalDivider(modifier: Modifier = Modifier, color: Color = FluentTheme.colors.stroke.divider.default,
    thickness: Dp = 1.dp) {
    androidx.compose.foundation.layout.Box(modifier.fillMaxWidth().height(thickness)
        .background(color))
}

@Composable
fun VerticalDivider(modifier: Modifier = Modifier, color: Color = FluentTheme.colors.stroke.divider.default,
    thickness: Dp = 1.dp) {
    Box(modifier.fillMaxHeight().width(thickness).background(color))
}

@Composable
fun CircularProgressIndicator(modifier: Modifier = Modifier, color: Color = FluentTheme.colors.fillAccent.default,
    strokeWidth: Dp = 3.dp) {
    ProgressRing(modifier = modifier, color = color, width = strokeWidth)
}

@Composable
fun LinearProgressIndicator(modifier: Modifier = Modifier, color: Color = FluentTheme.colors.fillAccent.default) {
    ProgressBar(modifier = modifier, color = color)
}

@Composable
fun LinearProgressIndicator(progress: () -> Float, modifier: Modifier = Modifier,
    color: Color = FluentTheme.colors.fillAccent.default) {
    ProgressBar(progress = progress().coerceIn(0f, 1f), modifier = modifier, color = color)
}

@Composable
fun DropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable MenuFlyoutScope.() -> Unit) {
    MenuFlyout(visible = expanded, onDismissRequest = onDismissRequest, content = content)
}

@Composable
fun MenuFlyoutScope.DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null) {
    MenuFlyoutItem(onClick = onClick, text = text, icon = leadingIcon, trailing = trailingIcon)
}

@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    shape: Shape = FluentTheme.shapes.overlay,
    containerColor: Color = FluentTheme.colors.background.solid.base,
    titleContentColor: Color = FluentTheme.colors.text.text.primary,
    textContentColor: Color = FluentTheme.colors.text.text.primary
) {
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Layer(modifier = modifier.widthIn(min = 320.dp, max = 540.dp), shape = shape, color = containerColor) {
            Column {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    icon?.invoke()
                    title?.let {
                        androidx.compose.runtime.CompositionLocalProvider(io.github.composefluent.LocalContentColor provides titleContentColor) {
                            ProvideTextStyle(FluentTheme.typography.subtitle, it)
                        }
                    }
                    text?.let {
                        androidx.compose.runtime.CompositionLocalProvider(io.github.composefluent.LocalContentColor provides textContentColor) { it() }
                    }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
fun OutlinedTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false, maxLines: Int = Int.MAX_VALUE, minLines: Int = 1,
    isError: Boolean = false, supportingText: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = FluentTheme.typography.body,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions()
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ProvideTextStyle(textStyle) {
            TextField(value, onValueChange, modifier = Modifier.fillMaxWidth().heightIn(min = (32 + (minLines - 1) * 20).dp),
                singleLine = singleLine, maxLines = maxLines, header = label, placeholder = placeholder,
                leadingIcon = leadingIcon, trailing = trailingIcon?.let { { it() } }, isClearable = false,
                colors = if (isError) TextFieldDefaults.defaultTextFieldColors(
                    default = TextFieldColor(
                        fillColor = FluentTheme.colors.control.default,
                        contentColor = FluentTheme.colors.text.text.primary,
                        placeholderColor = FluentTheme.colors.text.text.secondary,
                        bottomLineFillColor = FluentTheme.colors.system.critical,
                        borderBrush = SolidColor(FluentTheme.colors.system.critical),
                        cursorBrush = SolidColor(FluentTheme.colors.text.text.primary)
                    )
                ) else TextFieldDefaults.defaultTextFieldColors(),
                keyboardOptions = keyboardOptions, keyboardActions = keyboardActions)
        }
        androidx.compose.runtime.CompositionLocalProvider(io.github.composefluent.LocalContentColor provides
            if (isError) FluentTheme.colors.system.critical else FluentTheme.colors.text.text.secondary) {
            supportingText?.invoke()
        }
    }
}

@Composable
fun OutlinedTextField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier, singleLine: Boolean = false, placeholder: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions()) {
    TextField(value, onValueChange, modifier, singleLine = singleLine, placeholder = placeholder,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, isClearable = false)
}
