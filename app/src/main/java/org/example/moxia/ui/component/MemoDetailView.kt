package org.example.moxia.ui.component

import org.example.moxia.ui.theme.AppCircularProgressIndicator
import org.example.moxia.ui.theme.bottomBarBlurSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

import DeleteConfirmationDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.moxia.R
import org.example.moxia.ui.theme.AppDialog
import org.example.moxia.ui.theme.AppExtendedFloatingActionButton
import org.example.moxia.ui.theme.AppTextButton
import org.example.moxia.ui.theme.AppSurface
import org.example.moxia.ui.theme.AppTopBar
import org.example.moxia.model.isLinkTo
import org.example.moxia.model.Memo
import org.example.moxia.model.isLinkTo
import org.example.moxia.model.MemoRelation
import org.example.moxia.ui.component.item.markdown.plainLabel
import org.example.moxia.ui.component.composer.MemoComposerScreen
import org.example.moxia.ui.component.item.MemoItem
import org.example.moxia.viewmodel.MemosViewModel
import org.example.moxia.viewmodel.PaginatedListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoDetailView(
    modifier: Modifier = Modifier,
    memo: Memo,
    comments: PaginatedListState<Memo>,

    token: String,
    hostUrl: String = "",
    showBackButton: Boolean,
    onBack: () -> Unit,
    viewModel: MemosViewModel,
    reactionOptions: List<String> = emptyList(),
    /** Height the floating bottom bar covers here; the comment button must stay above it. */
    bottomBarInset: Dp = 0.dp,
    /** Opens a memo this one links to; null hides the tappability of the cards. */
    onOpenLinkedMemo: ((String) -> Unit)? = null,
    /** Removes one outgoing link; only offered to the memo's owner. */
    onRemoveLinkedMemo: ((String) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val onCommentMemo = LocalMemoCommenter.current
    val onEditMemo = LocalMemoEditor.current
    var memoToDelete by remember { mutableStateOf<Memo?>(null) }
    // Frosting needs API 33+; below that the comment button keeps a plain translucent fill.
    val blurAvailable = bottomBarBlurSupported()
    // Hoisted above the Scaffold: the content below records into it, the FAB above samples it.
    val detailBackdrop = rememberLayerBackdrop()

    val listState = rememberLazyListState()
    var isFabExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousScrollOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            isFabExpanded = when {
                index == 0 && offset == 0 -> true
                index > previousIndex -> false
                index < previousIndex -> true
                offset > previousScrollOffset + 10 -> false
                offset < previousScrollOffset - 10 -> true
                else -> isFabExpanded
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }

    val isOwner = remember(memo.creator, uiState.session.currUser?.name) {
        memo.creator == uiState.session.currUser?.name
    }
    // The detail only moves while it has something to reveal: no rubber band at either end.
    val overscroll = rememberPageOverscroll()

    AppSurface(
        modifier = modifier.fillMaxSize().clip(
            MaterialTheme.shapes.large.copy(
                bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp)
            )
        ),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0), topBar = {
                AppTopBar(
                    title = stringResource(R.string.memo_detail_title),
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.memo_detail_back)
                                )
                            }
                        }
                    }
                )
            }, floatingActionButton = {
                if (uiState.session.currUser != null) {
                    // The comment action floats over the memo text, so it can wear the full
                    // frosted material: the list layer is recorded by the pane scaffold above.
                    val commentBackdrop = if (blurAvailable) detailBackdrop else null
                    Box(Modifier.padding(bottom = bottomBarInset)) {
                        AppExtendedFloatingActionButton(
                            onClick = { onCommentMemo(memo) },
                            expanded = isFabExpanded,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            backdrop = commentBackdrop,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Add, contentDescription = null
                                )
                            },
                            text = {
                                Text(text = stringResource(R.string.memo_detail_add_comment))
                            }
                        )
                    }
                }
            }, containerColor = Color.Transparent, modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            // The FAB lives outside this lambda, so it never records itself into the layer it
            // samples.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (blurAvailable) Modifier.layerBackdrop(detailBackdrop) else Modifier)
                    .padding(innerPadding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    state = listState,
                    overscrollEffect = overscroll,
                    modifier = Modifier
                        .fillMaxSize() // Use fillMaxSize instead of fillMaxHeight
                        .widthIn(max = 800.dp)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp + bottomBarInset),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Original memo
                    item(key = "original_${memo.name ?: memo.content.hashCode()}") {
                        MemoItem(
                            memo = memo,
                            user = uiState.users[memo.creator],
                            currentUser = uiState.session.currUser,
                            token = token,
                            hostUrl = hostUrl,
                            colors = CardDefaults.cardColors(),
                            onEdit = if (isOwner) {
                                { onEditMemo(memo) }
                            } else null,
                            onPin = if (isOwner) { pinned ->
                                viewModel.memoActionDelegate.updateMemoPinned(memo, pinned)
                            } else null,
                            onDelete = if (isOwner) {
                                { memoToDelete = memo }
                            } else null,
                            onUpsertReaction = { emoji ->
                                viewModel.memoActionDelegate.upsertMemoReaction(memo, emoji)
                            },
                            onDeleteReaction = { reaction ->
                                viewModel.memoActionDelegate.deleteMemoReaction(memo, reaction)
                            },
                            onContentUpdate = if (isOwner) { newContent ->
                                viewModel.memoActionDelegate.updateMemo(
                                    memo,
                                    newContent,
                                    memo.visibility,
                                    memo.attachments ?: emptyList(),
                                    memo.location
                                )
                            } else null,
                            isDetailView = true,
                            headerScale = uiState.appSettings.headerScale,
                            reactionOptions = reactionOptions)
                    }

                    // Linked memos ("Link memo" in the editor). Only outgoing links belong here;
                    // incoming ones are shown on the memo they point at.
                    val linkedItems = memo.relations.orEmpty()
                        .filter { it.isLinkTo(memo.name) && it.relatedMemo.name.isNotBlank() }
                        .map { relation ->
                            val linkedName = relation.relatedMemo.name
                            val linked = (uiState.userMemoList.list.items + uiState.searchMemoList.list.items)
                                .firstOrNull { it.name == linkedName }
                            LinkedMemoItem(
                                name = linkedName,
                                // Rendered text, one line: a loaded memo gives its own content, an
                                // unloaded one falls back to the snippet the server sent.
                                label = linked?.plainLabel()
                                    ?: relation.relatedMemo.snippet?.takeIf { it.isNotBlank() }
                                    ?: linkedName
                            )
                        }
                    if (linkedItems.isNotEmpty()) {
                        item(key = "linked_${memo.name}") {
                            LinkedMemosCard(
                                items = linkedItems,
                                onOpen = onOpenLinkedMemo,
                                onRemove = if (isOwner) onRemoveLinkedMemo else null
                            )
                        }
                    }

                    // Comments section header
                    item(key = "comments_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.memo_detail_comments, comments.items.size
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Cached comments hint (offline: showing local cache)
                    if (comments.isOffline && !comments.isLoading) {
                        item(key = "comments_cached_hint") {
                            AppSurface(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.memo_detail_comments_cached),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Loading indicator for comments
                    if (comments.isLoading) {
                        item(key = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppCircularProgressIndicator()
                            }
                        }
                    }

                    // Comments list
                    if (!comments.isLoading && comments.items.isEmpty()) {
                        item(key = "empty_comments") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.memo_detail_no_comments),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Newest comment first: the conversation reads from the latest reply down.
                    // Pagination appends older pages to the end of `items`, so reversing the whole
                    // rendered list keeps that order as more pages arrive.
                    val orderedComments = comments.items.asReversed()
                    itemsIndexed(
                        orderedComments,
                        key = { index, it -> "comment_${it.name ?: it.content.hashCode()}_$index" }) { index, comment ->
                        val isCommentOwner = comment.creator == uiState.session.currUser?.name
                        MemoItem(
                            memo = comment,
                            user = uiState.users[comment.creator],
                            currentUser = uiState.session.currUser,
                            token = token,
                            hostUrl = hostUrl,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            onEdit = if (isCommentOwner) {
                                { onEditMemo(comment) }
                            } else null,
                            onDelete = if (isCommentOwner) {
                                { memoToDelete = comment }
                            } else null,
                            onUpsertReaction = { emoji ->
                                viewModel.memoActionDelegate.upsertMemoReaction(comment, emoji)
                            },
                            onDeleteReaction = { reaction ->
                                viewModel.memoActionDelegate.deleteMemoReaction(comment, reaction)
                            },
                            onContentUpdate = if (isCommentOwner) { newContent ->
                                viewModel.memoActionDelegate.updateMemo(
                                    comment,
                                    newContent,
                                    comment.visibility,
                                    comment.attachments ?: emptyList(),
                                    comment.location
                                )
                            } else null,
                            isDetailView = true,
                            headerScale = uiState.appSettings.headerScale,
                            reactionOptions = reactionOptions)
                    }
                }
            }
        }
    }

    memoToDelete?.let { m ->
        DeleteConfirmationDialog(memo = m, onDismiss = { memoToDelete = null }, onConfirm = {
            viewModel.memoActionDelegate.deleteMemo(m) {
                memoToDelete = null
                if (m == memo) onBack()
            }
        })
    }
}

/** One row of the linked-memos card: the memo's server name plus its one-line rendered label. */
private data class LinkedMemoItem(val name: String, val label: String)

/**
 * The memos this one points at. Kept out of the memo body: the relation lives on the server and is
 * independent of the text, exactly like the web client's "Link memo".
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun LinkedMemosCard(
    items: List<LinkedMemoItem>,
    onOpen: ((String) -> Unit)?,
    onRemove: ((String) -> Unit)? = null
) {
    var pendingRemoval by remember { mutableStateOf<Pair<String, String>?>(null) }
    AppSurface(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.memo_detail_linked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (onRemove != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.memo_detail_linked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items.forEach { item ->
                val name = item.name
                val label = item.label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        // Long press rather than a tiny ×: the row is small, and unlinking
                        // deserves a confirmation.
                        .combinedClickable(
                            onClick = { onOpen?.invoke(name) },
                            onLongClick = if (onRemove != null) {
                                { pendingRemoval = name to label }
                            } else null
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        // One line: this card is an index of connections, not a second feed.
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    val pending = pendingRemoval
    if (pending != null && onRemove != null) {
        AppDialog(
            show = true,
            title = stringResource(R.string.memo_unlink_title),
            summary = pending.second + "`n" + stringResource(R.string.memo_unlink_confirm),
            onDismissRequest = { pendingRemoval = null },
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { pendingRemoval = null }
                )
                AppTextButton(
                    text = stringResource(R.string.common_remove),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        pendingRemoval = null
                        onRemove(pending.first)
                    }
                )
            }
        )
    }
}