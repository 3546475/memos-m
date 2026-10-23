package org.example.memosm.ui.nav

import org.example.memosm.model.TopBarScrollMode
import org.example.memosm.model.EditGesture

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Drafts
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.memosm.R
import org.example.memosm.model.ThemeStyle
import org.example.memosm.ui.component.ActionSheet
import org.example.memosm.ui.component.ActionSheetItem
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppFilterChip
import org.example.memosm.ui.theme.AppTextButton
import org.example.memosm.ui.theme.LocalThemeStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import org.example.memosm.model.Memo
import org.example.memosm.ui.component.GenericMemosListPane
import org.example.memosm.ui.component.MemoSearchBar
import org.example.memosm.ui.component.MemosScaffold
import org.example.memosm.ui.component.SyncStatusChip
import org.example.memosm.ui.component.SyncStatus
import org.example.memosm.ui.component.SyncStatusIconButton
import org.example.memosm.ui.component.SyncStatusPanel
import org.example.memosm.ui.component.syncStatusChipLabel
import org.example.memosm.ui.component.syncStatusOf
import org.example.memosm.ui.component.composer.EditorRequest
import org.example.memosm.ui.component.composer.rememberMemoEditorLauncher
import org.example.memosm.ui.profile.DraftsActivity
import org.example.memosm.ui.profile.SettingsActivity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import org.example.memosm.ui.component.item.DraftsCard
import org.example.memosm.ui.component.rememberScrollContext
import org.example.memosm.viewmodel.MemosViewModel
import org.example.memosm.viewmodel.RefreshSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// The frosted disc clips the blur, so the radius can be generous.

@Composable
fun MemosScreen(
    viewModel: MemosViewModel,
    pagerState: PagerState,
    onToggleNavBar: ((Boolean) -> Unit)? = null,
    isNavBarVisible: Boolean = true,
    navBarAlwaysVisible: Boolean = false,
    topBarScrollMode: TopBarScrollMode = TopBarScrollMode.ON_SCROLL,
    memoEditGesture: EditGesture = EditGesture.NONE,
    openComposer: Boolean = false,
    onComposerOpened: () -> Unit = {},
    /** Off when the floating bottom bar carries its own compose button. */
    showComposeFab: Boolean = true,
    /** Shortcut request: expand the search field once, then report back. */
    openSearch: Boolean = false,
    onSearchOpened: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val miuixFeed = LocalThemeStyle.current == ThemeStyle.MIUIX
    val memosListState = rememberLazyListState()
    val exploreListState = rememberLazyListState()
    val activeFeed = MemoFeed.entries[pagerState.currentPage]
    val pagerScope = rememberCoroutineScope()
    // 探索 is a page of this feed, so Back leaves it for 笔记 before it leaves the app. Registered
    // here (inner composition) so it takes precedence over the destination-level handler.
    BackHandler(enabled = activeFeed == MemoFeed.EXPLORE) {
        pagerScope.launch {
            pagerState.animateScrollToPage(MemoFeed.entries.indexOf(MemoFeed.MEMOS))
        }
    }
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
    // The compose button opens the composer on tap; holding it reveals the other entries.
    var showQuickActions by remember { mutableStateOf(false) }
    var fabLongPressConsumed by remember { mutableStateOf(false) }
    var searchFromSheet by remember { mutableStateOf(false) }
    // Miuix keeps the sync status next to the search action instead of inside a
    // search field, so its panel is owned here.
    var showSyncPanel by remember { mutableStateOf(false) }
    val cacheSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAfterProfileDetails()
    }
    val composeAction: () -> Unit = {
        if (uiState.draft.drafts.isNotEmpty()) showDraftPrompt = true else composeNew()
    }
    fun onFabTap() {
        // A long press also reports an up event, so the trailing click must be swallowed.
        if (fabLongPressConsumed) fabLongPressConsumed = false else composeAction()
    }


    // FAB expansion based on scroll direction
    val scrollContext = rememberScrollContext(listState = listState)

    // Explicitly handle initial state or non-scroll updates if needed
    LaunchedEffect(listState, scrollContext.isScrollingDown) {
        isFabExpanded = !scrollContext.isScrollingDown
    }

    // The bars are edge-to-edge overlays: the system navigation area (including the gesture
    // handle) sits between them and the screen edge, so the content has to clear that inset too.
    val navigationBarInset =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val bottomPadding by animateDpAsState(
        targetValue = (if (isNavBarVisible) 80.dp else 16.dp) + navigationBarInset,
        label = "BottomPadding"
    )

    // The bottom bar floats over the content, so the detail pane must reserve its height to keep
    // its own floating button clear. Null callback means this layout has no bottom bar at all.
    val navBarInset by animateDpAsState(
        targetValue = if (onToggleNavBar != null && isNavBarVisible) {
            80.dp + navigationBarInset
        } else {
            0.dp
        },
        label = "NavBarInset"
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
        navBarAlwaysVisible = navBarAlwaysVisible,
        memoEditGesture = memoEditGesture,
        navBarInset = navBarInset,
        navigationKey = activeFeed,
        openSearch = openSearch || searchFromSheet,
        onSearchOpened = {
            searchFromSheet = false
            // The scaffold expanded the field on its own, so mirror it here: paging must stop
            // while the search panel is open even when the user never tapped the field.
            isSearchExpanded = true
            onSearchOpened()
        },
        listHeader = { showSearchBar, searchExpanded, onSearchExpandedChange ->
            if (miuixFeed) {
                // One row of chrome: text tabs on the left, search on the right. The
                // search field itself only exists while expanded, so the feed starts
                // near the top of the screen instead of under two stacked capsules.
                val rowVisible =
                    (topBarScrollMode == TopBarScrollMode.ALWAYS || showSearchBar) && !searchExpanded
                val tabsVisible = topBarScrollMode == TopBarScrollMode.ALWAYS || showSearchBar
                // Closing the search panel already fades it out above this row; growing the row
                // back in at the same time read as a second, shrinking motion. It appears in
                // place instead when the change was caused by the search panel closing.
                var wasSearchExpanded by remember { mutableStateOf(searchExpanded) }
                val returningFromSearch = wasSearchExpanded && !searchExpanded
                LaunchedEffect(searchExpanded) { wasSearchExpanded = searchExpanded }
                AnimatedVisibility(
                    visible = rowVisible,
                    enter = if (returningFromSearch) {
                        EnterTransition.None
                    } else {
                        fadeIn() + expandVertically()
                    },
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
                        // Miuix's feed header is a single fixed-height row, so the status
                        // text rides inline here instead of floating over the list: it can
                        // never grow the row and shift the feed.
                        if (syncStatusOf(uiState) != SyncStatus.IDLE) {
                            syncStatusChipLabel(uiState)?.let { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }
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
                // ON_SCROLL hides the tabs with the header; ALWAYS keeps them pinned.
                val tabsVisible = if (topBarScrollMode == TopBarScrollMode.ON_SCROLL) {
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
                val headerTop = if (miuixFeed) 64.dp else 88.dp + feedHeaderHeight
                val contentPadding = PaddingValues(
                    start = 16.dp,
                    top = headerTop,
                    end = 16.dp,
                    bottom = bottomPadding
                )
                // Keep the pull-to-refresh spinner under the header instead of behind its frost.
                // Miuix's refresh header is ~105dp tall, so the offset must clear the whole row.
                val indicatorPadding = if (miuixFeed) 72.dp else 0.dp
                when (feed) {
                    MemoFeed.MEMOS -> MemosListPane(
                        viewModel = viewModel,
                        listState = memosListState,
                        onMemoClick = onMemoClick,
                        contentPadding = contentPadding,
                        indicatorTopPadding = indicatorPadding,
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
                        indicatorTopPadding = indicatorPadding,
                        userProvider = { memo -> uiState.users[memo.creator] },
                        errorTitle = stringResource(R.string.common_error_failed_to_load_explore),
                        isActive = activeFeed == feed
                    )
                }
            }
        },
        overlay = { onMemoClick, showSearchBar, searchExpanded, onSearchExpandedChange, isDualPane, isDetailVisible ->
            val miuixSearch = miuixFeed
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
            // Sync/cache status floats over the feed instead of living in the
            // list's header item: a row that appears and disappears inside the
            // LazyColumn re-measures the header and shifts every memo card.
            // It is drawn before the search bar so the expanding panel covers
            // it, and it stays out of the way while search is open.
            // Offline is left out on purpose - the search bar's red cloud icon
            // already reports it.
            if (!miuixSearch && !searchExpanded) {
                SyncStatusChip(
                    uiState = uiState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp)
                )
            }

            if (miuixSearch) {
                searchBar()
            } else {
                AnimatedVisibility(
                    visible = if (topBarScrollMode == TopBarScrollMode.ALWAYS) {
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
            if (showComposeFab && uiState.session.currUser != null && !searchExpanded) {
                // Animate FAB position only if nav bar can be toggled (onToggleNavBar provided)
                val fabBottomPadding by animateDpAsState(
                    targetValue = (if (onToggleNavBar != null && isNavBarVisible) 96.dp else 16.dp) +
                        navigationBarInset,
                    label = "fabBottomPadding"
                )

                if (miuixFeed) {
                    // The compose button is always drawn here, never with Miuix's own FAB (it paints
                    // an inner squircle that shows up as a white block) and never frosted: the blur
                    // effect masks itself with the same squircle, which left a visible seam inside
                    // the circle. A small opaque circle on a shadow is what a FAB is anyway.
                    Box(
                        modifier = Modifier
                            .testTag("memo_feed_compose")
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = fabBottomPadding)
                            .fabLongPress { showQuickActions = true; fabLongPressConsumed = true }
                            .size(56.dp)
                            .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.primary)
                            .clickable(onClick = ::onFabTap),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.memo_composer_fab_new_memo),
                            // White on the day theme's dark green. The night primary is a light
                            // green, where a white plus would all but disappear, so it falls back
                            // to the palette's own content colour.
                            tint = if (MiuixTheme.colorScheme.primary.luminance() < 0.5f) {
                                Color.White
                            } else {
                                MiuixTheme.colorScheme.onPrimary
                            }
                        )
                    }
                } else {
                    ExtendedFloatingActionButton(
                    onClick = ::onFabTap,
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
                        .fabLongPress { showQuickActions = true; fabLongPressConsumed = true }
                    )
                }
            }
        })

    // Failed attachment uploads are only read when the panel is opened: they change rarely and the
    // list is a diagnostic view.
    LaunchedEffect(showSyncPanel) {
        if (showSyncPanel) viewModel.refreshFailedUploads()
    }

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
            },
            onRetryOp = { opId -> viewModel.retryPendingOp(opId) },
            onDiscardFailedUpload = { upload -> viewModel.discardFailedUpload(upload) }
        )
    }

    // Held compose button: the other ways into the feed without leaving it.
    if (showQuickActions) {
        ActionSheet(onDismissRequest = {
            showQuickActions = false
            fabLongPressConsumed = false
        }) {
            Text(
                text = stringResource(R.string.quick_actions_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
            ActionSheetItem(
                text = { Text(stringResource(R.string.memo_composer_fab_new_memo)) },
                leadingIcon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                onClick = {
                    showQuickActions = false
                    fabLongPressConsumed = false
                    composeAction()
                }
            )
            ActionSheetItem(
                text = { Text(stringResource(R.string.drafts_title)) },
                leadingIcon = { Icon(Icons.Outlined.Drafts, contentDescription = null) },
                onClick = {
                    showQuickActions = false
                    fabLongPressConsumed = false
                    draftsLauncher.launch(Intent(context, DraftsActivity::class.java))
                }
            )
            ActionSheetItem(
                text = { Text(stringResource(R.string.shortcut_search)) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                onClick = {
                    showQuickActions = false
                    fabLongPressConsumed = false
                    searchFromSheet = true
                }
            )
        }
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

/**
 * Observes a long press without consuming it, so the compose button keeps its own click, ripple
 * and shared transition. The click that a long press still produces is swallowed by the caller.
 *
 * The release is awaited with the raw pointer stream instead of [waitForUpOrCancellation]: that
 * helper reports `null` for a *consumed* up event, and the button's own `clickable` handles the
 * event first, so every plain tap looked like a long press.
 */
@Composable
private fun Modifier.fabLongPress(onLongPress: () -> Unit): Modifier {
    val current by rememberUpdatedState(onLongPress)
    val haptics = LocalHapticFeedback.current
    return pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                var up = false
                while (!up) {
                    up = awaitPointerEvent().changes.none { it.pressed }
                }
            }
            if (released == null) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                current()
            }
        }
    }
}

@Composable
private fun MemosListPane(
    viewModel: MemosViewModel,
    listState: LazyListState,
    onMemoClick: (Memo) -> Unit,
    contentPadding: PaddingValues,
    indicatorTopPadding: Dp = 0.dp,
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
        indicatorTopPadding = indicatorTopPadding,
        errorTitle = stringResource(R.string.common_error_failed_to_load_memos),
        onHashtagClick = onHashtagClick,
        isActive = isActive,
        header = {
            val hasShortcuts = uiState.userMemoList.shortcuts.isNotEmpty()
            val selectedHashtag = uiState.userMemoList.selectedHashtag
            val showFilterRow = hasShortcuts || selectedHashtag != null

            // Sync/cache status used to be the first row here. It now floats over
            // the pane (see MemosScreen's overlay): anything inside this header is
            // a LazyColumn item, so a row that appears and disappears re-measures
            // the header and shifts every memo card below it.
            if (hasDrafts || showFilterRow) {
                item(key = "header_section") {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
