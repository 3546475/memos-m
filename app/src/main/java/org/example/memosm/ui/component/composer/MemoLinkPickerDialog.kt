package org.example.memosm.ui.component.composer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.example.memosm.R
import org.example.memosm.model.Memo
import org.example.memosm.ui.component.item.markdown.plainLabel
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppListItem
import org.example.memosm.ui.theme.AppTextButton
import org.example.memosm.ui.theme.AppTextField


/**
 * Picks one memo to link to — the web client's "Link memo": search your own memos, choose one, and
 * the saved memo carries the connection.
 */
@Composable
fun MemoLinkPickerDialog(
    memos: List<Memo>,
    excludedNames: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (Memo) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val candidates = remember(memos, query, excludedNames) {
        val needle = query.trim()
        memos.filter { memo ->
            val name = memo.name
            name != null && name !in excludedNames &&
                (needle.isEmpty() || memo.content.contains(needle, ignoreCase = true))
        }
    }

    AppDialog(
        show = true,
        title = stringResource(R.string.memo_link_picker_title),
        summary = stringResource(R.string.memo_link_picker_summary),
        onDismissRequest = onDismiss,
        buttons = {
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            )
        }
    ) {
        AppTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.memo_link_picker_search),
            singleLine = true
        )
        if (candidates.isEmpty()) {
            Text(
                text = stringResource(R.string.memo_link_picker_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .padding(top = 8.dp)
            ) {
                items(candidates, key = { it.name ?: it.content.hashCode() }) { memo ->
                    AppListItem(
                        title = memo.plainLabel(),
                        summary = memo.name,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSelect(memo) }
                    )
                }
            }
        }
    }
}
