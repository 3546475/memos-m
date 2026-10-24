package org.example.moxia.ui.component.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.example.moxia.R

/**
 * The markdown rendering switches. They live under "编辑器" because they change how note content is
 * laid out when it is read or edited, not the app's chrome.
 */
@Composable
fun EditorSettingsCard(
    codeLineNumbers: Boolean,
    codeWrapLines: Boolean,
    paragraphIndent: Boolean,
    cjkSpacing: Boolean,
    onCodeLineNumbersChange: (Boolean) -> Unit,
    onCodeWrapLinesChange: (Boolean) -> Unit,
    onParagraphIndentChange: (Boolean) -> Unit,
    onCjkSpacingChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SettingToggleRow(
            label = stringResource(R.string.settings_editor_line_numbers),
            description = stringResource(R.string.settings_editor_line_numbers_description),
            checked = codeLineNumbers,
            onCheckedChange = onCodeLineNumbersChange
        )
        SettingToggleRow(
            label = stringResource(R.string.settings_editor_wrap_lines),
            description = stringResource(R.string.settings_editor_wrap_lines_description),
            checked = codeWrapLines,
            onCheckedChange = onCodeWrapLinesChange
        )
        SettingToggleRow(
            label = stringResource(R.string.settings_editor_paragraph_indent),
            description = stringResource(R.string.settings_editor_paragraph_indent_description),
            checked = paragraphIndent,
            onCheckedChange = onParagraphIndentChange
        )
        SettingToggleRow(
            label = stringResource(R.string.settings_editor_cjk_spacing),
            description = stringResource(R.string.settings_editor_cjk_spacing_description),
            checked = cjkSpacing,
            onCheckedChange = onCjkSpacingChange
        )
    }
}
