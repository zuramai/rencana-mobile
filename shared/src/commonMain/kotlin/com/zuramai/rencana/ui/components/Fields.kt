package com.zuramai.rencana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType

/**
 * Rencana text field: caption [label] above a 52 dp-high, radius 14, 1.5 dp #F0E6DA bordered white box.
 * [modifier] is applied to the actual input (put `Modifier.testTag("...")` and `fillMaxWidth()` there), so
 * uiautomator `input text` reaches it. [containerModifier] sizes the whole label+field column (e.g. `weight`). [supportingText] renders below (muted, or red when [isError]).
 */
@Composable
fun RencanaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val type = RencanaType.current
    Column(containerModifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, style = type.caption)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = type.body,
            cursorBrush = SolidColor(RColors.Primary),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = !password && keyboardType == KeyboardType.Text && capitalization != KeyboardCapitalization.None,
                keyboardType = if (password) KeyboardType.Password else keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .background(RColors.Surface, RShapes.Field)
                        .border(1.5.dp, if (isError) RColors.Danger else RColors.Border, RShapes.Field)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) Text(placeholder, style = type.body.copy(color = RColors.Muted.copy(alpha = 0.6f)))
                        inner()
                    }
                    trailing?.invoke()
                }
            }
        )
        if (supportingText != null) {
            Text(supportingText, style = type.caption.copy(color = if (isError) RColors.Danger else RColors.Muted), modifier = Modifier.fillMaxWidth())
        }
    }
}
