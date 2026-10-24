package org.example.moxia.ui.component.setting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.example.moxia.R
import org.example.moxia.data.audit.SyncAuditEntry
import org.example.moxia.data.audit.SyncAuditLogger
import org.example.moxia.ui.theme.AppDialog
import org.example.moxia.ui.theme.AppFilterChip
import org.example.moxia.ui.theme.AppListItem
import org.example.moxia.ui.theme.AppTextButton
import org.koin.core.context.GlobalContext
import java.text.DateFormat
import java.util.Date

/**
 * Read-only viewer for the persistent sync audit log, grouped by day the way every other settings
 * page groups its rows: one rounded card per day, plain rows inside it.
 *
 * The log only receives entries when something actually has to be queued, retried, uploaded or
 * restored (see [SyncAuditLogger]), which is why it can look empty on a device that never went
 * offline; the empty state says so instead of leaving the user guessing.
 */
@Composable
fun AuditLogContent(modifier: Modifier = Modifier) {
    val auditLogger: SyncAuditLogger = remember { GlobalContext.get().get() }
    val entries by remember(auditLogger) { auditLogger.observeRecent(MAX_ENTRIES) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedCategory by rememberSaveable { mutableStateOf(LogCategory.ALL) }
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    val filteredEntries = entries.filter { selectedCategory.matches(it) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dayGroups = filteredEntries.groupBy { formatAuditDay(it.occurredAt) }.toList()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "filters") {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogCategory.entries.forEach { category ->
                    AppFilterChip(
                        selected = category == selectedCategory,
                        onClick = { selectedCategory = category },
                        content = { Text(stringResource(category.labelRes)) }
                    )
                }
            }
        }

        // The page exists to be pasted into a bug report, so exporting is the primary action.
        item(key = "actions") {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                AuditLogAction(
                    label = stringResource(R.string.audit_log_copy),
                    enabled = filteredEntries.isNotEmpty()
                ) {
                    copyAuditEntries(context, filteredEntries)
                }
                Spacer(Modifier.width(16.dp))
                AuditLogAction(
                    label = stringResource(R.string.audit_log_clear),
                    enabled = entries.isNotEmpty()
                ) { confirmingClear = true }
            }
        }

        if (filteredEntries.isEmpty()) {
            item(key = "empty") {
                Column(Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
                    SettingsGroup {
                        AppListItem(
                            title = stringResource(R.string.audit_log_empty),
                            summary = stringResource(R.string.audit_log_empty_hint)
                        )
                    }
                }
            }
        } else {
            itemsIndexed(dayGroups, key = { _, group -> group.first }) { _, group ->
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsLabel(group.first)
                    SettingsGroup {
                        group.second.forEach { entry -> AuditLogItem(entry) }
                    }
                }
            }
        }
    }

    AppDialog(
        show = confirmingClear,
        title = stringResource(R.string.audit_log_clear),
        summary = stringResource(R.string.audit_log_clear_confirm),
        onDismissRequest = { confirmingClear = false },
        buttons = {
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = { confirmingClear = false }
            )
            AppTextButton(
                text = stringResource(R.string.audit_log_clear),
                modifier = Modifier.weight(1f),
                onClick = {
                    confirmingClear = false
                    scope.launch { auditLogger.clear() }
                }
            )
        }
    )
}

@Composable
private fun AuditLogAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        },
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun AuditLogItem(entry: SyncAuditEntry) {
    val detail = listOfNotNull(entry.operation, entry.detailCode)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    AppListItem(
        title = "${entry.event} · ${entry.outcome}",
        supporting = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatAuditTime(entry.occurredAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.accountHash.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.audit_log_account, entry.accountHash),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!entry.targetHash.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.audit_log_target, entry.targetHash),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

private fun copyAuditEntries(context: Context, entries: List<SyncAuditEntry>) {
    val text = entries.joinToString("\n") { entry ->
        listOfNotNull(
            formatAuditTime(entry.occurredAt),
            entry.event,
            entry.outcome,
            entry.operation?.takeIf { it.isNotBlank() },
            entry.detailCode?.takeIf { it.isNotBlank() },
            entry.accountHash.takeIf { it.isNotBlank() }?.let { "account=$it" },
            entry.targetHash?.takeIf { it.isNotBlank() }?.let { "target=$it" }
        ).joinToString("  ")
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("sync log", text))
    Toast.makeText(
        context,
        context.getString(R.string.audit_log_copy_done, entries.size),
        Toast.LENGTH_SHORT
    ).show()
}

private fun formatAuditDay(timestamp: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(timestamp))

private fun formatAuditTime(timestamp: Long): String =
    DateFormat.getTimeInstance(DateFormat.MEDIUM).format(Date(timestamp))

/**
 * Filter categories for the sync log, matched against
 * [SyncAuditEntry.event]. Unknown events fall back to [SYSTEM].
 */
private enum class LogCategory(val labelRes: Int) {
    ALL(R.string.log_filter_all),
    SYNC(R.string.log_filter_sync),
    QUEUE(R.string.log_filter_queue),
    ATTACHMENT(R.string.log_filter_attachment),
    RECOVERY(R.string.log_filter_recovery),
    SYSTEM(R.string.log_filter_system);

    fun matches(entry: SyncAuditEntry): Boolean =
        this == ALL || this == categoryOf(entry.event)

    private companion object {
        fun categoryOf(event: String): LogCategory = when (event) {
            "QUEUE" -> QUEUE
            "SYNC", "WORKER" -> SYNC
            "ATTACHMENT_UPLOAD" -> ATTACHMENT
            "EXPORT", "IMPORT" -> RECOVERY
            else -> SYSTEM // DATABASE_CORRUPTED and anything unknown
        }
    }
}

private const val MAX_ENTRIES = 100
