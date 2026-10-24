package org.example.moxia.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.example.moxia.R
import org.example.moxia.data.sync.PreDownloadState
import org.example.moxia.ui.preDownloadPhaseLabel
import org.example.moxia.viewmodel.MemosUiState

/**
 * Transient sync/cache status, floating over the memo feed's overlay layer.
 *
 * It used to be the first row of the list's header item, which meant every
 * appearance and disappearance re-measured the lazy list's header and shifted
 * all memo cards by the row's height. Drawn here it is not part of the list's
 * layout at all, so it can fade in and out without moving anything.
 *
 * Offline is deliberately not reported: the search bar's cloud icon already
 * turns red for it, and staying offline is a state, not an event worth a
 * floating pill on top of the content. Idle is equally silent.
 *
 * [appearDelayMillis] hides the other kind of flicker: an empty offline queue
 * still flips `SyncManager.isSyncing` for a few milliseconds on startup, and a
 * pass that short is not worth a chip.
 */
@Composable
fun SyncStatusChip(
    uiState: MemosUiState,
    modifier: Modifier = Modifier,
    appearDelayMillis: Long = 400L
) {
    val label = syncStatusChipLabel(uiState)
    val wanted = label != null

    var visible by remember { mutableStateOf(false) }
    // Keep the text while the exit animation runs; the state is already gone by
    // the time the chip fades out.
    var shownLabel by remember { mutableStateOf(label) }
    LaunchedEffect(wanted) {
        if (!wanted) {
            visible = false
        } else {
            delay(appearDelayMillis)
            visible = true
        }
    }
    LaunchedEffect(label) {
        if (label != null) shownLabel = label
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)) + slideInVertically(tween(150)) { -it / 2 },
        exit = fadeOut(tween(200)),
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = syncChipIcon(syncStatusOf(uiState)),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = shownLabel.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * One-line text for the sync status, or null when the state must stay silent
 * (idle, offline). Shared by the floating chip and the Miuix header row, which
 * has no room for a floating pill and reports the same state inline.
 */
@Composable
fun syncStatusChipLabel(uiState: MemosUiState): String? = when (syncStatusOf(uiState)) {
    SyncStatus.SYNCING -> stringResource(R.string.sync_status_syncing)

    SyncStatus.PREDOWNLOAD -> stringResource(
        R.string.sync_status_predownload,
        preDownloadPhaseLabel((uiState.preDownloadState as PreDownloadState.Running).phase)
    )

    SyncStatus.PENDING -> stringResource(R.string.sync_status_pending, uiState.pendingOpsCount)

    SyncStatus.RATE_LIMITED -> stringResource(R.string.sync_status_rate_limited)

    SyncStatus.OFFLINE, SyncStatus.IDLE -> null
}

private fun syncChipIcon(status: SyncStatus): ImageVector = when (status) {
    SyncStatus.PREDOWNLOAD -> Icons.Outlined.Download
    SyncStatus.SYNCING -> Icons.Outlined.Sync
    else -> Icons.Outlined.CloudSync
}
