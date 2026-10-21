package org.example.memosm.ui.nav

import org.example.memosm.model.TopBarScrollMode

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.example.memosm.R
import org.example.memosm.model.ThemeStyle
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppFilterChip
import org.example.memosm.ui.theme.AppTextButton
import org.example.memosm.ui.theme.LocalThemeStyle
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFloatingActionButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import org.example.memosm.data.sync.PreDownloadState
import org.example.memosm.model.Memo
import org.example.memosm.ui.component.GenericMemosListPane
import org.example.memosm.ui.component.MemoSearchBar
import org.example.memosm.ui.component.MemosScaffold
import org.example.memosm.ui.component.SyncStatusBar
import org.example.memosm.ui.component.SyncStatus
import org.example.memosm.ui.component.SyncStatusIconButton
import org.example.memosm.ui.component.SyncStatusPanel
import org.example.memosm.ui.component.syncStatusOf
import org.example.memosm.ui.component.composer.EditorRequest
import org.example.memosm.ui.component.composer.rememberMemoEditorLauncher
import org.example.memosm.ui.profile.DraftsActivity
import org.example.memosm.ui.profile.SettingsActivity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import org.example.memosm.ui.component.item.DraftsCard
import org.example.memosm.ui.component.rememberScrollContext
import org.example.memosm.viewmodel.MemosViewModel
import org.example.memosm.viewmodel.RefreshSource

@Composable
fun MemosScreen(
    viewModel: MemosViewModel,
    pagerState: PagerState,
    onToggleNavBar: ((Boolean) -> Unit)? = null,
    isNavBarVisible: Boolean = true,
    topBarScrollMode: TopBarScrollMode = TopBarScrollMode.ALL,
    openComposer: Boolean = false,
    onComposerOpened: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val memosListState = rememberLazyListState()
    val exploreListState = rememberLazyListState()
    val activeFeed = MemoFeed.entries[pagerState.currentPage]
    val listState = if (activeFeed == MemoFeed.MEMOS) memosListState else exploreListState
    val activeMemos = if (activeFeed == MemoFeed.MEMOS) {
        uiState.userMemoList.list.items
    } else {
        uiState.exploreMemoList.list.items
    }
    var isSearchExpanded by remember(activeFeed) { mutableStateOf(false) }
    val density = LocalDensity.current
    var feedHeaderHeight by remember { mutableStateOf(64.dp) }
    val context = LocalContext.current
    val openEditor = rememberMemoEditorLauncher(viewModel)
    val draftsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAfterEditor()
    }
    val composeNew: () -> Unit = {
        uiState.accounts.firstOrNull { it.isActive }?.let { account ->
            openEditor(EditorRequest(account.id, titleRes = R.string.memo_composer_fab_new_memo,
                visibility = uiState.session.userSettings?.memoVisibility ?: org.example.memosm.model.Visibility.PRIVATE))
        }
    }
    var showDraftPrompt by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(true) }
    // Miuix keeps the sync status next to the search action instead of inside a
    // search field, so its panel is owned here.
    var showSyncPanel by remember { mutableStateOf(false) }
    val cacheSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAfterProfileDetails()
    }


    // FAB expansion based on scroll direction
    val scrollContext = rememberScrollContext(listState = listState)

    // Explicitly handle initial state or non-scroll updates if needed
    LaunchedEffect(listState, scrollContext.isScrollingDown) {
        isFabExpanded = !scrollContext.isScrollingDown
    }

    val bottomPadding by animateDpAsState(
        targetValue = if (isNavBarVisible) 80.dp else 16.dp, label = "BottomPadding"
    )

    // Handle external composer open request (e.g. from widget)
    LaunchedEffect(openComposer) {
        if (openComposer) {
            // Same logic as FAB click
            if (uiState.draft.drafts.isNotEmpty()) {
                showDraftPrompt = true
            } else {
                composeNew()
            }
            onComposerOpened()
        }
    }

    // Double tap refresh logic: scroll to top
    var lastProcessedTrigger by remember { mutableLongStateOf(uiState.refreshTrigger) }
    LaunchedEffect(uiState.refreshTrigger) {
        val activeRefreshSource = if (activeFeed == MemoFeed.MEMOS) {
            RefreshSource.USerMemos
        } else {
            RefreshSource.ExploreMemos
        }
        if (uiState.refreshTrigger > lastProcessedTrigger && uiState.refreshSource == activeRefreshSource) {
            listState.animateScrollToItem(0)
        }
        lastProcessedTrigger = uiState.refreshTrigger
    }

    MemosScaffold(
        viewModel = viewModel,
        memos = activeMemos,
        listState = listState,
        onToggleNavBar = { onToggleNavBar?.invoke(it) },
        isNavBarVisible = isNavBarVisible,
        navigationKey = activeFeed,
        listHeader = { showSearchBar, searchExpanded, onSearchExpandedChange ->
            if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
                // One row of chrome: text tabs on the left, search on the right. The
                // search field itself only exists while expanded, so the feed starts
                // near the top of the screen instead of under two stacked capsules.
                val rowVisible =
                    (topBarScrollMode != TopBarScrollMode.ALL || showSearchBar) && !searchExpanded
                val tabsVisible = topBarScrollMode == TopBarScrollMode.NEVER || showSearchBar
                AnimatedVisibility(
                    visible = rowVisible && !searchExpanded,
                    enter = fadeIn() + expandVertically(),
                    // Expanding the search replaces the row outright, so it must not
                    // animate the pane downwards while the panel animates in.
                    exit = if (searchExpanded) ExitTransition.None else fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(visible = tabsVisible) {
                            MemoFeedTabs(pagerState, Modifier.testTag("memo_feed_tabs"))
                        }
                        Spacer(Modifier.weight(1f))
                        // An idle sync state carries no information, so it stays silent.
                        if (syncStatusOf(uiState) != SyncStatus.IDLE) {
                            SyncStatusIconButton(
                                uiState = uiState,
                                onClick = { showSyncPanel = true }
                            )
                        }
                        IconButton(onClick = { onSearchExpandedChange(true) }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.memo_search_placeholder)
                            )
                        }
                    }
                }
            } else {
                // ALL hides the tabs with the search bar; the other modes keep the tabs pinned.
                val tabsVisible = if (topBarScrollMode == TopBarScrollMode.ALL) {
                    showSearchBar && !searchExpanded
                } else {
                    !searchExpanded
                }
                AnimatedVisibility(
                    visible = tabsVisible,
                    enter = slideInVertically { -it } + expandVertically() + fadeIn(),
                    exit = slideOutVertically { -it } + shrinkVertically() + fadeOut()
                ) {
                    MemoFeedTabs(
                        pagerState,
                        Modifier
                            .testTag("memo_feed_tabs")
                            .onSizeChanged {
                                feedHeaderHeight = with(density) { it.height.toDp() }
                            }
                            .padding(bottom = 16.dp)
                    )
                }
            }
        },
        listPane = { onMemoClick ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize().testTag("memo_feed_pager"),
                userScrollEnabled = !isSearchExpanded && !showDraftPrompt,
                key = { MemoFeed.entries[it] }
            ) { page ->
                val feed = MemoFeed.entries[page]
                // Miuix's header is a single row, so the list only needs to clear it;
                // MD keeps the tab row plus the floating search field below it.
                val contentPadding = PaddingValues(
                    start = 16.dp,
                    top = if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
                        64.dp
                    } else {
                        88.dp + feedHeaderHeight
                    },
                    end = 16.dp,
                    bottom = bottomPadding
                )
                when (feed) {
                    MemoFeed.MEMOS -> MemosListPane(
                        viewModel = viewModel,
                        listState = memosListState,
                        onMemoClick = onMemoClick,
                        contentPadding = contentPadding,
                        onDraftsCardClick = { draftsLauncher.launch(Intent(context, DraftsActivity::class.java)) },
                        onHashtagClick = { tag -> viewModel.shortcutDelegate.toggleHashtagFilter(tag) },
                        isActive = activeFeed == feed
                    )
                    MemoFeed.EXPLORE -> GenericMemosListPane(
                        viewModel = viewModel,
                        modifier = Modifier.testTag("memo_feed_explore"),
                        memos = uiState.exploreMemoList.list.items,
                        isLoading = uiState.exploreMemoList.list.isLoading,
                        isRefreshing = uiState.isRefreshing && uiState.refreshSource == RefreshSource.ExploreMemos,
                        nextPageToken = uiState.exploreMemoList.list.nextPageToken,
                        onLoadMore = { viewModel.loadMoreExploreMemos() },
                        onRefresh = { viewModel.fetchExploreMemos(refresh = true) },
                        onMemoClick = onMemoClick,
                        listState = exploreListState,
                        contentPadding = contentPadding,
                        userProvider = { memo -> uiState.users[memo.creator] },
                        errorTitle = stringResource(R.string.common_error_failed_to_load_explore),
                        isActive = activeFeed == feed
                    )
                }
            }
        },
        overlay = { onMemoClick, showSearchBar, searchExpanded, onSearchExpandedChange, isDualPane, isDetailVisible ->
            val miuixSearch = LocalThemeStyle.current == ThemeStyle.MIUIX
            val searchBar: @Composable () -> Unit = {
                key(activeFeed) {
                    MemoSearchBar(
                        viewModel = viewModel,
                        isExplore = activeFeed == MemoFeed.EXPLORE,
                        placeholder = stringResource(
                            if (activeFeed == MemoFeed.EXPLORE) R.string.memo_search_explore_placeholder
                            else R.string.memo_search_placeholder
                        ),
                        onMemoClick = onMemoClick,
                        // Miuix has no collapsed field: the header row's search action
                        // drives the expanded panel, so the state is hoisted there.
                        expandedOverride = if (miuixSearch) searchExpanded else null,
                        onExpandedChange = {
                            isSearchExpanded = it
                            onSearchExpandedChange(it)
                        }
                    )
                }
            }
            if (miuixSearch) {
                searchBar()
            } else {
                AnimatedVisibility(
                    visible = if (topBarScrollMode == TopBarScrollMode.NEVER) {
                        !searchExpanded || isDualPane || !isDetailVisible
                    } else {
                        (showSearchBar || searchExpanded) && (!searchExpanded || isDualPane || !isDetailVisible)
                    },
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter)) {
                    searchBar()
                }
            }

            // FAB for creating new memo
            if (uiState.session.currUser != null && !searchExpanded) {
                // Animate FAB position only if nav bar can be toggled (onToggleNavBar provided)
                val fabBottomPadding by animateDpAsState(
                    targetValue = if (onToggleNavBar != null && isNavBarVisible) 96.dp else 16.dp,
                    label = "fabBottomPadding"
                )

                if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
                    MiuixFloatingActionButton(
                        onClick = {
                            if (uiState.draft.drafts.isNotEmpty()) {
                                showDraftPrompt = true
                            } else {
                                composeNew()
                            }
                        },
                        modifier = Modifier
                            .testTag("memo_feed_compose")
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = fabBottomPadding)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.memo_composer_fab_new_memo),
                            tint = MiuixTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    ExtendedFloatingActionButton(
                    onClick = {
                        // If drafts exist, show prompt; otherwise show composer directly
                        if (uiState.draft.drafts.isNotEmpty()) {
                            showDraftPrompt = true
                        } else {
                            // Start fresh with a new draft session ID
                            composeNew()
                        }
                    },
                    expanded = isFabExpanded,
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = if (isFabExpanded) null
                                else stringResource(R.string.memo_composer_fab_new_memo)
                        )
                    },
                    text = {
                        Text(text = stringResource(R.string.memo_composer_fab_new_memo))
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .testTag("memo_feed_compose")
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = fabBottomPadding)
                    )
                }
            }
        })

    if (showSyncPanel) {
        SyncStatusPanel(
            uiState = uiState,
            onDismiss = { showSyncPanel = false },
            onDeleteOp = { opId -> viewModel.deletePendingOp(opId) },
            onManageCache = {
                showSyncPanel = false
                cacheSettingsLauncher.launch(
                    SettingsActivity.createIntent(context, SettingsSection.OFFLINE)
                )
            }
        )
    }

    // Draft prompt dialog
    AppDialog(
        show = showDraftPrompt,
        title = stringResource(R.string.drafts_prompt_title),
        summary = stringResource(R.string.drafts_prompt_message),
        onDismissRequest = { showDraftPrompt = false },
        buttons = {
            AppTextButton(
                text = stringResource(R.string.drafts_prompt_continue),
                modifier = Modifier.weight(1f),
                onClick = {
                    showDraftPrompt = false
                    // Load latest draft
                    val latestDraft = viewModel.draftDelegate.getLatestDraft()
                    val account = uiState.accounts.firstOrNull { it.isActive }
                    if (latestDraft != null && account != null) {
                        openEditor(EditorRequest(account.id, titleRes = R.string.memo_composer_fab_new_memo,
                            draft = latestDraft))
                    } else composeNew()
                }
            )
            AppTextButton(
                text = stringResource(R.string.drafts_prompt_start_fresh),
                modifier = Modifier.weight(1f),
                onClick = {
                    showDraftPrompt = false
                    // Start fresh with a new draft session ID
                    composeNew()
                }
            )
        }
    )

}

@Composable
private fun MemosListPane(
    viewModel: MemosViewModel,
    listState: LazyListState,
    onMemoClick: (Memo) -> Unit,
    contentPadding: PaddingValues,
    onDraftsCardClick: () -> Unit,
    onHashtagClick: (String) -> Unit,
    isActive: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasDrafts = uiState.draft.drafts.isNotEmpty()
    val shortcutListState = rememberLazyListState()
    val draftCount = uiState.draft.drafts.size

    GenericMemosListPane(
        viewModel = viewModel,
        modifier = Modifier.testTag("memo_feed_memos"),
        memos = uiState.userMemoList.list.items,
        isLoading = uiState.userMemoList.list.isLoading,
        isRefreshing = uiState.isRefreshing && uiState.refreshSource == RefreshSource.USerMemos,
        nextPageToken = uiState.userMemoList.list.nextPageToken,
        onLoadMore = { viewModel.loadMoreUserMemos() },
        onRefresh = { viewModel.fetchUserMemos(refresh = true) },
        onMemoClick = onMemoClick,
        listState = listState,
        contentPadding = contentPadding,
        errorTitle = stringResource(R.string.common_error_failed_to_load_memos),
        onHashtagClick = onHashtagClick,
        isActive = isActive,
        header = {
            val hasShortcuts = uiState.userMemoList.shortcuts.isNotEmpty()
            val selectedHashtag = uiState.userMemoList.selectedHashtag
            val showFilterRow = hasShortcuts || selectedHashtag != null

            // Sync status: visible only while an activity is ongoing (syncing,
            // pre-downloading, pending writes, offline). Once everything settles
            // it animates away (shrinks) and the compact status icon at the
            // search bar takes over - the check mark appears there. It lives
            // inside the header_section item (not its own LazyColumn item) so
            // the list's 8dp item spacing does not leave a gap after it hides.
            val syncActive = uiState.isSyncing ||
                uiState.preDownloadState is PreDownloadState.Running ||
                uiState.pendingOpsCount > 0 ||
                !uiState.isOnline ||
                uiState.connectionState == org.example.memosm.viewmodel.ConnectionState.RATE_LIMITED

            if (hasDrafts || showFilterRow || syncActive) {
                item(key = "header_section") {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = syncActive,
                            enter = androidx.compose.animation.fadeIn() +
                                androidx.compose.animation.expandVertically(),
                            exit = androidx.compose.animation.fadeOut() +
                                androidx.compose.animation.shrinkVertically()
                        ) {
                            SyncStatusBar(
                                isOnline = uiState.isOnline,
                                isSyncing = uiState.isSyncing,
                                pendingOpsCount = uiState.pendingOpsCount,
                                preDownloadState = uiState.preDownloadState,
                                cachedCount = uiState.textCacheCount,
                                lastSyncTime = uiState.lastSyncTime,
                                connectionState = uiState.connectionState
                            )
                        }

                        // Drafts Card (shown when drafts exist)
                        AnimatedVisibility(
                            visible = hasDrafts,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            DraftsCard(
                                draftCount = draftCount,
                                viewModel = viewModel,
                                onCardClick = onDraftsCardClick
                            )
                        }

                        // Horizontal Shortcut Row
                        AnimatedVisibility(
                            visible = showFilterRow,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LazyRow(
                                state = shortcutListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        val startGradient = Brush.horizontalGradient(
                                            0f to Color.Transparent, 0.15f to Color.Black
                                        )
                                        val endGradient = Brush.horizontalGradient(
                                            0.85f to Color.Black, 1f to Color.Transparent
                                        )
                                        if (shortcutListState.canScrollBackward) {
                                            drawRect(
                                                brush = startGradient, blendMode = BlendMode.DstIn
                                            )
                                        }
                                        if (shortcutListState.canScrollForward) {
                                            drawRect(
                                                brush = endGradient, blendMode = BlendMode.DstIn
                                            )
                                        }
                                    },
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (selectedHashtag != null) {
                                    item {
                                        AppFilterChip(
                                            selected = true,
                                            onClick = {
                                                viewModel.shortcutDelegate.toggleHashtagFilter(
                                                    selectedHashtag
                                                )
                                            },
                                            content = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Tag,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(selectedHashtag.removePrefix("#"))
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                itemsIndexed(uiState.userMemoList.shortcuts, key = { index, it ->
                                    val baseKey = it.name.takeUnless { n -> n.isNullOrBlank() }
                                        ?: "${it.title?.hashCode() ?: 0}_${it.filter?.hashCode() ?: 0}"
                                    "${baseKey}_$index"
                                }) { index, shortcut ->
                                    val isSelected =
                                        uiState.userMemoList.selectedShortcut?.name == shortcut.name
                                    AppFilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.shortcutDelegate.toggleShortcutFilter(
                                                shortcut
                                            )
                                        },
                                        content = {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.Shortcut,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(shortcut.title ?: "")
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        })
                                }
                            }
                        }
                    }
                }
            }
        })

}
