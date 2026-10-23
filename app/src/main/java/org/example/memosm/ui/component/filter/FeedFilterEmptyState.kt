package org.example.memosm.ui.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.memosm.R
import org.example.memosm.ui.theme.AppButton
import org.example.memosm.ui.theme.AppTextButton

/**
 * What the feed shows instead of a blank pane. A filtered feed that returns nothing used to look
 * like the app had failed to load; the empty state names the situation and offers both exits
 * (clear the filter, or change it).
 */
@Composable
fun FeedEmptyState(
    filtered: Boolean,
    onClearFilter: () -> Unit,
    onChangeFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (filtered) Icons.Outlined.FilterList else Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(
                if (filtered) R.string.memo_filter_empty_title else R.string.memo_list_empty_title
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (filtered) R.string.memo_filter_empty_message else R.string.memo_list_empty_message
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (filtered) {
            Spacer(Modifier.height(20.dp))
            AppButton(onClick = onClearFilter) {
                Text(stringResource(R.string.memo_filter_clear_action))
            }
            Spacer(Modifier.height(4.dp))
            AppTextButton(
                text = stringResource(R.string.memo_filter_change_action),
                onClick = onChangeFilter
            )
        }
    }
}
