package org.example.memosm.ui.component

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.memosm.R
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppDotGlyph
import org.example.memosm.ui.theme.AppDotLoader
import org.example.memosm.ui.theme.AppTextButton
import org.example.memosm.api.GsonProvider
import org.example.memosm.data.media.AttachmentUpload
import org.example.memosm.data.sync.PendingOp
import org.example.memosm.data.sync.PendingOpType
import org.example.memosm.data.sync.PreDownloadState
import org.example.memosm.ui.component.setting.SettingsGroup
import org.example.memosm.ui.component.setting.SettingsNavigationRow
import org.example.memosm.ui.preDownloadPhaseLabel
import org.example.memosm.viewmodel.MemosUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Footprint of the compact sync glyph in the icon slot. */
private val SyncGlyphSize = 20.dp

// Static 5x5 dot-matrix glyphs for the resting sync states. Cells are indexed row * 5 + column,
// row 0 at the top, matching the grid the loading wave animates on.
private val IdleGlyph = setOf(9, 13, 17, 15, 21) // check mark: up to date
private val PendingGlyph = setOf(2, 6, 7, 8, 12, 17, 22) // arrow up: writes waiting to be sent
private val OfflineGlyph = setOf(0, 4, 6, 8, 12, 16, 18, 20, 24) // cross: no connection
private val LimitedGlyph = setOf(6, 11, 16, 8, 13, 18) // pause: server busy, retrying

/**
 * Status icon rendered inside the search bar's trailing-icon slot: a plain
 * icon button so it shares the search bar's internal vertical centering (no
 * separate Surface that can drift off the search bar's center line). One
 * glance tells the sync/cache state; tapping opens the detail panel.
 */
@Composable
fun SyncStatusIconButton(
    uiState: MemosUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = syncStatusOf(uiState)
    IconButton(onClick = onClick, modifier = modifier) {
        val glyph = Modifier.size(SyncGlyphSize)
        when (status) {
            SyncStatus.SYNCING, SyncStatus.PREDOWNLOAD -> {
                // The same dot-matrix wave as the app's loading indicator, shrunk into the icon
                // slot: a ring here read as a different kind of activity from every other load.
                AppDotLoader(modifier = glyph)
            }

            // The four resting states use static dot-matrix glyphs on the same 5x5 grid, so the
            // whole status family speaks one visual language.
            SyncStatus.PENDING -> {
                val label = stringResource(R.string.sync_status_pending, uiState.pendingOpsCount)
                AppDotGlyph(
                    cells = PendingGlyph,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = glyph.semantics { contentDescription = label }
                )
            }

            SyncStatus.OFFLINE -> {
                val label = stringResource(R.string.sync_status_offline)
                // Error red: the icon is the only persistent offline cue the user asked to
                // keep (the banner below the search bar stays quiet).
                AppDotGlyph(
                    cells = OfflineGlyph,
                    color = MaterialTheme.colorScheme.error,
                    modifier = glyph.semantics { contentDescription = label }
                )
            }

            SyncStatus.RATE_LIMITED -> {
                val label = stringResource(R.string.sync_status_rate_limited)
                AppDotGlyph(
                    cells = LimitedGlyph,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = glyph.semantics { contentDescription = label }
                )
            }

            SyncStatus.IDLE -> {
                val label = stringResource(R.string.sync_status_idle)
                AppDotGlyph(
                    cells = IdleGlyph,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = glyph.semantics { contentDescription = label }
                )
            }
        }
    }
}

/**
 * Essential sync status, pending changes and cache actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusPanel(
    uiState: MemosUiState,
    onDismiss: () -> Unit,
    onDeleteOp: (String) -> Unit,
    onManageCache: () -> Unit,
    onRetryOp: (String) -> Unit = {},
    onDiscardFailedUpload: (AttachmentUpload) -> Unit = {}
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            // Main status row (larger than the old caption-sized row)
            Row(verticalAlignment = Alignment.CenterVertically) {
                val status = syncStatusOf(uiState)
                val icon = when (status) {
                    SyncStatus.OFFLINE -> Icons.Outlined.CloudOff
                    SyncStatus.IDLE -> Icons.Outlined.CheckCircle
                    else -> Icons.Outlined.CloudSync
                }
                val statusText: String = when (status) {
                    SyncStatus.SYNCING ->
                        stringResource(R.string.offline_sync_status_syncing)
                    SyncStatus.PREDOWNLOAD -> {
                        val phase = (uiState.preDownloadState as PreDownloadState.Running).phase
                        stringResource(
                            R.string.sync_status_predownload,
                            preDownloadPhaseLabel(phase)
                        )
                    }
                    SyncStatus.PENDING ->
                        stringResource(R.string.sync_status_pending, uiState.pendingOpsCount)
                    SyncStatus.OFFLINE ->
                        stringResource(R.string.sync_status_offline)
                    SyncStatus.RATE_LIMITED ->
                        stringResource(R.string.sync_status_rate_limited)
                    SyncStatus.IDLE ->
                        stringResource(R.string.sync_status_idle)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(
                            R.string.sync_panel_last_sync,
                            if (uiState.lastSyncTime <= 0L) stringResource(R.string.sync_time_never)
                            else DateUtils.getRelativeTimeSpanString(
                                uiState.lastSyncTime, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
                            ).toString()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Pending ops queue
            if (uiState.pendingOps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.sync_panel_queue, uiState.pendingOps.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                uiState.pendingOps.forEach { op ->
                    PendingOperationItem(
                        op = op,
                        onDelete = { onDeleteOp(op.id) },
                        onRetry = { onRetryOp(op.id) }
                    )
                }
            }

            // Uploads that will never make it (server rejection or too many attempts). The memo
            // went on without them, so the only useful action is discarding the staged bytes —
            // but they are listed instead of disappearing silently.
            if (uiState.failedUploads.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    stringResource(R.string.sync_panel_failed_uploads, uiState.failedUploads.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                uiState.failedUploads.forEach { upload ->
                    FailedUploadItem(upload = upload, onDiscard = { onDiscardFailedUpload(upload) })
                }
            }

            // Pre-download progress
            Spacer(modifier = Modifier.height(12.dp))
            when (val state = uiState.preDownloadState) {
                is PreDownloadState.Running -> {
                    Text(
                        text = stringResource(
                            R.string.sync_panel_predownload_running,
                            preDownloadPhaseLabel(state.phase),
                            state.page
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Indeterminate: the total page count is unknown, and a
                    // "1/page" determinate value would walk BACKWARDS as pages
                    // accumulate, which reads as a broken progress bar.
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is PreDownloadState.Failed -> {
                    Text(
                        text = stringResource(R.string.offline_predownload_failed, state.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> Unit
            }

            Spacer(modifier = Modifier.height(16.dp))
            SettingsGroup {
                SettingsNavigationRow(
                    title = stringResource(R.string.sync_panel_manage_cache),
                    icon = Icons.Outlined.Storage,
                    onClick = onManageCache
                )
            }
        }
    }
}

@Composable
private fun PendingOperationItem(op: PendingOp, onDelete: () -> Unit, onRetry: () -> Unit) {
    var expanded by remember(op.id) { mutableStateOf(false) }
    // Deleting a queued op discards an unsynced user write, so it needs an
    // explicit confirmation (same AlertDialog pattern as RecoveryCard).
    var confirmDelete by remember(op.id) { mutableStateOf(false) }
    val target = op.memoName ?: op.parentName ?: "-"
    val payloadSummary = remember(op.payloadJson) { pendingPayloadSummary(op.payloadJson) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (op.permanentlyFailed) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = opTypeLabel(op),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = payloadSummary ?: target,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = stringResource(
                        if (expanded) R.string.sync_queue_collapse else R.string.sync_queue_expand
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Only an op the server already rejected needs the manual retry: the others are
                // scheduled (and retried) automatically. Retrying clears the failure
                // bookkeeping, so the row also stops being reported as needing attention.
                if (op.permanentlyFailed) {
                    IconButton(onClick = onRetry) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.sync_queue_retry),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.offline_sync_delete_op),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(6.dp))
                PendingOperationDetail(
                    label = stringResource(R.string.sync_queue_target),
                    value = target
                )
                PendingOperationDetail(
                    label = stringResource(R.string.sync_queue_queued_at),
                    value = formatOperationTime(op.createdAt)
                )
                PendingOperationDetail(
                    label = stringResource(R.string.sync_queue_attempts),
                    value = op.attemptCount.toString()
                )
                if (op.lastAttemptAt > 0L) {
                    PendingOperationDetail(
                        label = stringResource(R.string.sync_queue_last_attempt),
                        value = formatOperationTime(op.lastAttemptAt)
                    )
                }
                if (!op.updateMask.isNullOrBlank()) {
                    PendingOperationDetail(
                        label = stringResource(R.string.sync_queue_fields),
                        value = op.updateMask
                    )
                }
                if (!op.lastError.isNullOrBlank()) {
                    PendingOperationDetail(
                        label = stringResource(R.string.sync_queue_error),
                        value = op.lastError,
                        isError = true
                    )
                }
                if (op.permanentlyFailed) {
                    Text(
                        text = stringResource(R.string.sync_queue_needs_attention),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AppDialog(
            show = true,
            title = stringResource(R.string.sync_queue_delete_title),
            summary = stringResource(R.string.sync_queue_delete_message),
            onDismissRequest = { confirmDelete = false },
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { confirmDelete = false }
                )
                AppTextButton(
                    text = stringResource(R.string.offline_sync_delete_op),
                    modifier = Modifier.weight(1f),
                    destructive = true,
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                )
            }
        )
    }
}

@Composable
private fun FailedUploadItem(upload: AttachmentUpload, onDiscard: () -> Unit) {
    var confirmDiscard by remember(upload.id) { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = upload.filename,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = upload.lastError ?: stringResource(R.string.sync_queue_needs_attention),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { confirmDiscard = true }) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.offline_upload_discard),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (confirmDiscard) {
        AppDialog(
            show = true,
            title = stringResource(R.string.offline_upload_discard),
            summary = stringResource(R.string.offline_upload_discard_message, upload.filename),
            onDismissRequest = { confirmDiscard = false },
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { confirmDiscard = false }
                )
                AppTextButton(
                    text = stringResource(R.string.offline_upload_discard),
                    modifier = Modifier.weight(1f),
                    destructive = true,
                    onClick = {
                        confirmDiscard = false
                        onDiscard()
                    }
                )
            }
        )
    }
}

@Composable
private fun PendingOperationDetail(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(92.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun pendingPayloadSummary(payloadJson: String?): String? {
    if (payloadJson.isNullOrBlank()) return null
    return runCatching {
        val payload = GsonProvider.gson.fromJson(payloadJson, com.google.gson.JsonObject::class.java)
        payload.get("content")?.takeUnless { it.isJsonNull }?.asString
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(120)
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun formatOperationTime(timestamp: Long): String =
    SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

@Composable
private fun opTypeLabel(op: PendingOp): String {
    val type = runCatching { PendingOpType.valueOf(op.type) }.getOrDefault(PendingOpType.UPDATE)
    return stringResource(
        when (type) {
            PendingOpType.CREATE -> R.string.offline_op_create
            PendingOpType.UPDATE -> R.string.offline_op_update
            PendingOpType.DELETE -> R.string.offline_op_delete
            PendingOpType.COMMENT_CREATE -> R.string.offline_op_comment_create
            PendingOpType.REACTION_UPSERT -> R.string.offline_op_reaction_upsert
            PendingOpType.REACTION_DELETE -> R.string.offline_op_reaction_delete
            PendingOpType.ATTACHMENT_DELETE -> R.string.offline_op_attachment_delete
        }
    )
}
