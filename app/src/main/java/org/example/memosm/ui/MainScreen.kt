package org.example.memosm.ui

import androidx.activity.compose.BackHandler
import org.example.memosm.model.NavBarScrollMode

import org.example.memosm.model.AppearancePreferences

import org.example.memosm.ui.theme.isMiuixMotion

import org.example.memosm.ui.theme.appSlideSpec

import org.example.memosm.ui.theme.appFadeSpec

import top.yukonga.miuix.kmp.theme.MiuixTheme

import org.example.memosm.ui.theme.bottomBarBlurSupported

import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

import top.yukonga.miuix.kmp.blur.layerBackdrop

import top.yukonga.miuix.kmp.blur.drawBackdrop

import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown


import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.State
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Attachment
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.filled.Map
import org.example.memosm.model.MapCapability
import org.example.memosm.ui.nav.MapScreen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.graphics.vector.ImageVector
import org.example.memosm.model.ThemeStyle
import org.example.memosm.ui.theme.AppSurface
import org.example.memosm.ui.theme.capsuleHighlight
import org.example.memosm.ui.theme.LocalThemeStyle
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import org.example.memosm.ui.component.item.media.AccountMediaIdentity
import org.example.memosm.ui.component.item.media.LocalAccountMediaIdentity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.launch
import kotlin.math.pow
import org.example.memosm.R
import org.example.memosm.model.Attachment
import org.example.memosm.model.ShareIntentData
import org.example.memosm.model.Visibility
import org.example.memosm.ui.component.ConflictDialog
import org.example.memosm.ui.component.LoginDialog
import org.example.memosm.ui.component.LocalNetworkPermission
import org.example.memosm.ui.theme.LocalBottomBarBlurEnabled
import org.example.memosm.ui.theme.LocalTopBarBlurEnabled
import org.example.memosm.ui.component.composer.EditorRequest
import org.example.memosm.ui.component.composer.rememberMemoEditorLauncher
import org.example.memosm.ui.component.item.media.MemoImage
import org.example.memosm.ui.component.item.markdown.LocalLinkPreviews
import org.example.memosm.ui.component.item.markdown.LocalMarkdownFormat
import org.example.memosm.ui.component.item.markdown.MarkdownFormat
import org.example.memosm.ui.component.item.markdown.LinkPreviewEnvironment
import org.example.memosm.ui.component.resolveResourceUrl
import org.example.memosm.ui.nav.AttachmentsScreen
import org.example.memosm.ui.nav.MemoFeed
import org.example.memosm.ui.nav.MemosScreen
import org.example.memosm.ui.nav.ProfileScreen
import org.example.memosm.viewmodel.MemosViewModel
import org.example.memosm.viewmodel.MemosUiState
import androidx.compose.runtime.MutableState

enum class MainDestination(
    val labelRes: Int
) {
    MEMOS(R.string.nav_memos), MAP(R.string.nav_map), ATTACHMENTS(R.string.nav_attachments), PROFILE(
        R.string.nav_profile
    )
}

private const val NavBlurRadius = 24f

/** Opacity of the capsule's own surface over its blur, the same in the light and dark themes. */
private const val CapsuleSurfaceAlpha = 0.86f

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    appearance: AppearancePreferences,
    modifier: Modifier = Modifier,
    shareIntentData: ShareIntentData? = null,
    onShareIntentConsumed: () -> Unit = {},
    shouldOpenComposer: Boolean = false,
    onComposerOpened: () -> Unit = {},
    /** One-shot tab request from a shortcut or widget, consumed by the screen. */
    destinationRequest: MainDestination? = null,
    onDestinationRequestConsumed: () -> Unit = {},
    /** One-shot search request; [MemosScreen] consumes it after expanding the field. */
    openSearchRequest: Boolean = false,
    onOpenSearchRequestConsumed: () -> Unit = {}
) {
    val destination = rememberSaveable { mutableStateOf(MainDestination.MEMOS) }
    val viewModel: MemosViewModel = org.koin.compose.koinInject()
    val uiState by viewModel.uiState.collectAsState()
    val previewRepository by viewModel.linkPreviews.collectAsState()
    val linkPreviews = remember(previewRepository, uiState.appSettings.linkPreviewEnabled, uiState.isOnline) {
        previewRepository?.takeIf { uiState.appSettings.linkPreviewEnabled }
            ?.let { LinkPreviewEnvironment(it, uiState.isOnline) }
    }
    val mediaIdentity = uiState.accounts.firstOrNull { it.isActive }?.let {
        AccountMediaIdentity(it.id, uiState.accountGeneration)
    }
    val markdownFormat = remember(
        uiState.appSettings.codeLineNumbers,
        uiState.appSettings.codeWrapLines,
        uiState.appSettings.paragraphIndent,
        uiState.appSettings.cjkSpacing
    ) {
        MarkdownFormat(
            codeLineNumbers = uiState.appSettings.codeLineNumbers,
            codeWrapLines = uiState.appSettings.codeWrapLines,
            paragraphIndent = uiState.appSettings.paragraphIndent,
            cjkSpacing = uiState.appSettings.cjkSpacing
        )
    }
    CompositionLocalProvider(
        LocalAccountMediaIdentity provides mediaIdentity,
        LocalLinkPreviews provides linkPreviews,
        LocalMarkdownFormat provides markdownFormat
    ) {
        key(uiState.accountGeneration) {
            MainScreenContent(viewModel, uiState, destination, onLogout, appearance, modifier,
                shareIntentData, onShareIntentConsumed, shouldOpenComposer, onComposerOpened,
                destinationRequest, onDestinationRequestConsumed,
                openSearchRequest, onOpenSearchRequestConsumed)
        }
    }
}

@Composable
private fun MainScreenContent(
    viewModel: MemosViewModel,
    uiState: MemosUiState,
    destination: MutableState<MainDestination>,
    onLogout: () -> Unit,
    appearance: AppearancePreferences,
    modifier: Modifier,
    shareIntentData: ShareIntentData?,
    onShareIntentConsumed: () -> Unit,
    shouldOpenComposer: Boolean,
    onComposerOpened: () -> Unit,
    destinationRequest: MainDestination?,
    onDestinationRequestConsumed: () -> Unit,
    openSearchRequest: Boolean,
    onOpenSearchRequestConsumed: () -> Unit
) {
    var currentDestination by destination
    // Back returns to the notes feed instead of leaving the app from any other destination. It is
    // declared before the content so the inner handlers (search panel, map selection, memo detail)
    // are registered later and therefore get the first chance to consume the gesture.
    BackHandler(enabled = currentDestination != MainDestination.MEMOS) {
        currentDestination = MainDestination.MEMOS
    }
    val mapSupported = uiState.memoMap.capability == MapCapability.SUPPORTED
    // The user can hide, reorder and drop bottom-bar destinations; MAP only exists when supported.
    val availableDestinations = remember(mapSupported) {
        MainDestination.entries.filter { it != MainDestination.MAP || mapSupported }
    }
    val visibleDestinations = remember(appearance.navDestinations, availableDestinations) {
        if (appearance.navDestinations.isEmpty()) {
            availableDestinations
        } else {
            appearance.navDestinations
                .mapNotNull { name -> availableDestinations.firstOrNull { it.name == name } }
                .ifEmpty { availableDestinations }
        }
    }
    LaunchedEffect(mapSupported, uiState.accountGeneration) {
        if (!mapSupported && currentDestination == MainDestination.MAP) currentDestination = MainDestination.MEMOS
    }
    val feedPagerState = rememberPagerState(pageCount = { MemoFeed.entries.size })

    // Shortcuts and widgets can name a tab. Consume the request once so a later recomposition
    // (or a configuration change) does not keep forcing the user back to that tab.
    LaunchedEffect(destinationRequest) {
        if (destinationRequest != null) {
            if (destinationRequest == MainDestination.MEMOS) {
                feedPagerState.requestScrollToPage(MemoFeed.MEMOS.ordinal)
            }
            currentDestination = destinationRequest
            onDestinationRequestConsumed()
        }
    }

    // The search field lives inside the Memos destination, so the shortcut has to land there
    // first; MemosScreen reports back once the field is actually expanded.
    LaunchedEffect(openSearchRequest) {
        if (openSearchRequest) {
            currentDestination = MainDestination.MEMOS
            feedPagerState.requestScrollToPage(MemoFeed.MEMOS.ordinal)
        }
    }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val networkPermission = LocalNetworkPermission.current
    var previouslyGranted by remember { mutableStateOf(networkPermission.granted) }
    LaunchedEffect(networkPermission.granted) {
        if (networkPermission.granted && !previouslyGranted) {
            viewModel.retryAccountConnection()
        }
        previouslyGranted = networkPermission.granted
    }

    val saveableStateHolder = rememberSaveableStateHolder()

    val navBarAlwaysVisible = appearance.navBarScrollMode == NavBarScrollMode.ALWAYS_VISIBLE
    var isNavBarVisible by remember { mutableStateOf(true) }
    // The bar waits for the notes feed's first settled content instead of appearing over the
    // skeleton: the feed reports when its placeholders are gone, then the bar draws two frames
    // later (its blur needs a drawn backdrop). Every other destination, and a feed that never
    // settles, reveals it as before.
    var feedSettled by remember { mutableStateOf(false) }
    var bottomBarReady by remember { mutableStateOf(false) }
    LaunchedEffect(currentDestination, feedSettled) {
        if (currentDestination != MainDestination.MEMOS || feedSettled) {
            withFrameNanos { }
            withFrameNanos { }
            bottomBarReady = true
        }
    }
    LaunchedEffect(currentDestination) {
        if (currentDestination == MainDestination.MEMOS && !bottomBarReady) {
            // Fail-safe: an offline start that never finishes a first page must not hide the bar.
            delay(3_000)
            bottomBarReady = true
        }
    }

    LaunchedEffect(navBarAlwaysVisible) { if (navBarAlwaysVisible) isNavBarVisible = true }
    var isAddingAccount by remember { mutableStateOf(false) }

    val openEditor = rememberMemoEditorLauncher(viewModel)

    // Track if we've already processed the current share intent
    var processedShareData by remember { mutableStateOf<ShareIntentData?>(null) }

    // Trigger composer when share data is received - CREATE NEW draft

    // Switch to Memos tab if widget triggered composer
    LaunchedEffect(shouldOpenComposer) {
        if (shouldOpenComposer) {
            currentDestination = MainDestination.MEMOS
            // The pager may be off-screen when a widget request arrives.
            feedPagerState.requestScrollToPage(MemoFeed.MEMOS.ordinal)
        }
    }

    LaunchedEffect(shareIntentData) {
        // Only process if:
        // 1. We have share data
        // 2. We haven't already processed this exact share data
        if (shareIntentData != null && !shareIntentData.isEmpty && processedShareData != shareIntentData) {
            val account = uiState.accounts.firstOrNull { it.isActive } ?: return@LaunchedEffect
            openEditor(EditorRequest(account.id, titleRes = R.string.memo_composer_fab_new_memo,
                content = shareIntentData.text.orEmpty(), uris = shareIntentData.uris.map { it.toString() },
                visibility = uiState.session.userSettings?.memoVisibility ?: Visibility.PRIVATE))
            processedShareData = shareIntentData
            onShareIntentConsumed()
        }
    }

    DisposableEffect(currentDestination) {
        focusManager.clearFocus()
        onDispose { }
    }

    val configuration = LocalConfiguration.current
    val isMobile = configuration.screenWidthDp < 600

    val miuixMotion = isMiuixMotion(LocalThemeStyle.current)

    // The bottom bar blurs a snapshot of the content layer, and neither that snapshot nor the bar
    // is invalidated by the destination cross-fade on its own. This transition ticks on every frame
    // of that fade; both the content layer and the bar read it, so the backdrop is re-recorded and
    // re-sampled instead of freezing on the page that was just left.
    val barBlurTransition = updateTransition(currentDestination, label = "BottomBarBlurRefresh")
    val barBlurRefresh = barBlurTransition.animateFloat(
        // Fixed duration (not the spring used by the content fade): the value is only an
        // invalidation tick, and the page cross-fade has settled well inside this window even
        // for adjacent tabs, whose ordinal delta would otherwise finish too early.
        transitionSpec = { tween(durationMillis = 500) },
        label = "BottomBarBlurRefreshValue"
    ) { destination -> destination.ordinal.toFloat() }

    val adaptiveInfo = androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2()


    // The explicit setter is for detail/search visibility (always honoured). The scroll-aware
    // setter only governs scroll-driven hiding, so "always visible" does not pin the bar while
    // reading a memo detail or searching.
    val setNavBarVisible: ((Boolean) -> Unit)? = if (isMobile) {
        { visible -> isNavBarVisible = visible }
    } else null
    val toggleNavBar: ((Boolean) -> Unit)? = if (isMobile) {
        { visible -> if (!navBarAlwaysVisible) isNavBarVisible = visible }
    } else null


    if (isAddingAccount) {
        LoginDialog(onLoginSuccess = { newBaseUrl, newToken ->
            scope.launch {
                viewModel.userDelegate.addAccount(newBaseUrl, newToken)
                isAddingAccount = false
            }
        }, onDismiss = { isAddingAccount = false })
    }


    @Composable
    fun NavigationIcon(
        destination: MainDestination, isSelected: Boolean, modifier: Modifier = Modifier.size(24.dp)
    ) {
        when (destination) {
            MainDestination.MAP -> Icon(
                if (isSelected) Icons.Filled.Map else Icons.Outlined.Map,
                contentDescription = null,
                modifier = modifier
            )
            MainDestination.MEMOS -> Icon(
                if (isSelected) Icons.AutoMirrored.Filled.LibraryBooks else Icons.AutoMirrored.Outlined.LibraryBooks,
                contentDescription = null,
                modifier = modifier
            )

            MainDestination.ATTACHMENTS -> Icon(
                if (isSelected) Icons.Default.Attachment else Icons.Outlined.Attachment,
                contentDescription = null,
                modifier = modifier
            )

            MainDestination.PROFILE -> {
                val user = uiState.session.currUser
                val account = uiState.accounts.find { it.isActive }
                val rawAvatarUrl = user?.avatarUrl ?: account?.avatarUrl
                val hostUrl = uiState.session.hostUrl

                val avatarUri = remember(rawAvatarUrl, hostUrl) {
                    if (rawAvatarUrl.isNullOrBlank()) Uri.EMPTY
                    else (resolveResourceUrl(hostUrl, rawAvatarUrl) ?: "").toUri()
                }

                MemoImage(
                    attachment = null,
                    token = uiState.session.token,
                    hostUrl = hostUrl,
                    uri = avatarUri,
                    contentDescription = null,
                    isRound = true,
                    placeholderIcon = if (isSelected) Icons.Default.Person else Icons.Outlined.Person,
                    modifier = modifier
                        .then(
                            if (isSelected) Modifier.border(
                                2.dp, MaterialTheme.colorScheme.primary, CircleShape
                            ) else Modifier
                        )
                        .padding(if (isSelected) 1.dp else 0.dp)
                )
            }
        }
    }

    fun handleDestinationClick(destination: MainDestination) {
        focusManager.clearFocus()
        val currentTime = System.currentTimeMillis()
        if (currentDestination == destination && currentTime - lastTapTime < 500) {
            when (destination) {
                MainDestination.MEMOS -> when (MemoFeed.entries[feedPagerState.currentPage]) {
                    MemoFeed.MEMOS -> viewModel.fetchUserMemos(refresh = true)
                    MemoFeed.EXPLORE -> viewModel.fetchExploreMemos(refresh = true)
                }
                MainDestination.ATTACHMENTS -> viewModel.fetchAttachments(refresh = true)
                MainDestination.MAP -> viewModel.memoMapManager.load()
                else -> {}
            }
        }
        currentDestination = destination
        lastTapTime = currentTime
    }

    // One backdrop per destination. The bar samples only the destination it is showing, so the
    // outgoing page's fade layer never bleeds into the bar's blur while the page cross-fades.
    val memoBackdrop = rememberLayerBackdrop()
    val mapBackdrop = rememberLayerBackdrop()
    val attachmentsBackdrop = rememberLayerBackdrop()
    val profileBackdrop = rememberLayerBackdrop()
    fun backdropFor(destination: MainDestination): LayerBackdrop = when (destination) {
        MainDestination.MEMOS -> memoBackdrop
        MainDestination.MAP -> mapBackdrop
        MainDestination.ATTACHMENTS -> attachmentsBackdrop
        MainDestination.PROFILE -> profileBackdrop
    }
    // The destination page records its backdrop in the same frame it is first composed. Delay the
    // bar's switch by one frame: switching immediately sampled a still-empty layer for an instant,
    // which made the blur vanish and the sharp content flash through the translucent bar.
    var activeNavBackdrop by remember { mutableStateOf(backdropFor(currentDestination)) }
    LaunchedEffect(currentDestination) {
        withFrameNanos { }
        activeNavBackdrop = backdropFor(currentDestination)
    }

    AppSurface(
        modifier = modifier.fillMaxSize(), containerColor = MaterialTheme.colorScheme.background
    ) {
        // Each bar has its own frosted switch; the values are provided here so every screen below
        // reads the same preference without threading it through every composable.
        CompositionLocalProvider(
            LocalTopBarBlurEnabled provides appearance.topBarBlur,
            LocalBottomBarBlurEnabled provides appearance.bottomBarBlur
        ) {
        val navBlurEnabled = bottomBarBlurSupported()
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                // Navigation Rail for tablets/desktops
                if (!isMobile) {
                    if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
                        MiuixNavigationRail {
                            Spacer(Modifier.height(12.dp))
                            visibleDestinations.forEach { destination ->
                                if (destination == MainDestination.PROFILE) {
                                    Spacer(Modifier.weight(1f))
                                }
                                MiuixNavigationRailItem(
                                    selected = currentDestination == destination,
                                    onClick = { handleDestinationClick(destination) },
                                    icon = destination.navIcon(currentDestination == destination),
                                    label = stringResource(destination.labelRes)
                                )
                            }
                        }
                    } else {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Spacer(Modifier.height(12.dp))
                            // Items
                            visibleDestinations.forEach { destination ->
                                if (destination == MainDestination.PROFILE) {
                                    Spacer(Modifier.weight(1f))
                                }
                                NavigationRailItem(
                                    selected = currentDestination == destination,
                                    onClick = { handleDestinationClick(destination) },
                                    icon = {
                                        NavigationIcon(
                                            destination, currentDestination == destination
                                        )
                                    },
                                    label = { Text(stringResource(destination.labelRes)) })
                            }
                        }
                    }
                }

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Content
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = {
                            val spec = appFadeSpec(miuixMotion)
                            // Same-level bottom destinations have no spatial order, so they get a
                            // fade-through with a subtle zoom instead of a horizontal slide (which
                            // would falsely imply a stack push). The outgoing page still leaves
                            // quickly so it cannot linger in the translucent bar's backdrop.
                            (fadeIn(animationSpec = spec) + scaleIn(
                                animationSpec = spec,
                                initialScale = if (miuixMotion) 0.96f else 0.92f,
                                transformOrigin = TransformOrigin.Center
                            )) togetherWith fadeOut(animationSpec = tween(90))
                        },
                        label = "MainScreenDestinationTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetDestination ->
                        val destinationKey = if (targetDestination == MainDestination.MAP) {
                            "map:${uiState.accounts.firstOrNull { it.isActive }?.id}:${uiState.session.hostUrl}"
                        } else targetDestination
                        Box(
                            Modifier
                                .fillMaxSize()
                                .then(
                                    if (navBlurEnabled) {
                                        Modifier.layerBackdrop(backdropFor(targetDestination))
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            saveableStateHolder.SaveableStateProvider(destinationKey) {
                            when (targetDestination) {
                                MainDestination.MAP -> if (mapSupported) {
                                    key(uiState.accountGeneration) {
                                        MapScreen(viewModel, isNavBarVisible = isNavBarVisible, onToggleNavBar = setNavBarVisible, navBarAlwaysVisible = navBarAlwaysVisible)
                                    }
                                }
                                MainDestination.MEMOS -> MemosScreen(
                                    viewModel = viewModel,
                                    pagerState = feedPagerState,
                                    topBarScrollMode = appearance.topBarScrollMode,
                                    onToggleNavBar = setNavBarVisible,
                                    isNavBarVisible = isNavBarVisible,
                                    navBarAlwaysVisible = navBarAlwaysVisible,
                                    memoEditGesture = appearance.editGesture,
                                    // The floating bar carries the compose action already.
                                    showComposeFab = !(
                                        LocalThemeStyle.current == ThemeStyle.MIUIX &&
                                            appearance.floatingNavBar
                                        ),
                                    openComposer = shouldOpenComposer,
                                    onComposerOpened = onComposerOpened,
                                    openSearch = openSearchRequest,
                                    onSearchOpened = onOpenSearchRequestConsumed,
                                    onFeedSettled = { feedSettled = true }
                                )

                                MainDestination.ATTACHMENTS -> AttachmentsScreen(
                                    viewModel = viewModel,
                                    onToggleNavBar = toggleNavBar,
                                    isNavBarVisible = isNavBarVisible
                                )

                                MainDestination.PROFILE -> ProfileScreen(
                                    viewModel = viewModel,
                                    onLogout = onLogout,
                                    onAddAccount = { isAddingAccount = true },
                                    onToggleNavBar = toggleNavBar,
                                    isNavBarVisible = isNavBarVisible,
                                    onOpenMemos = {
                                        handleDestinationClick(MainDestination.MEMOS)
                                        feedPagerState.requestScrollToPage(MemoFeed.MEMOS.ordinal)
                                    }
                                )
                            }
                        }
                        }
                    }
                }
            }

            // Bottom Navigation Bar for mobile
            if (isMobile) {
                val floatingBar =
                    LocalThemeStyle.current == ThemeStyle.MIUIX && appearance.floatingNavBar
                if (floatingBar) {
                    FloatingBottomBar(
                        visible = isNavBarVisible && bottomBarReady,
                        destinations = visibleDestinations,
                        current = currentDestination,
                        onDestination = { handleDestinationClick(it) },
                        onCompose = {
                            uiState.accounts.firstOrNull { it.isActive }?.let { account ->
                                openEditor(
                                    EditorRequest(
                                        account.id,
                                        titleRes = R.string.memo_composer_fab_new_memo,
                                        visibility = uiState.session.userSettings?.memoVisibility
                                            ?: Visibility.PRIVATE
                                    )
                                )
                            }
                        },
                        blurEnabled = navBlurEnabled,
                        backdrop = activeNavBackdrop,
                        blurRefresh = barBlurRefresh,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else {
                // The docked bar keeps its own navigation-bar inset: unlike the floating capsule,
                // it should fill the gesture-handle row with the same solid/blurred surface.
                Box(Modifier.align(Alignment.BottomCenter)) {
                    AnimatedVisibility(
                        visible = isNavBarVisible && bottomBarReady,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(140)),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
                            MiuixNavigationBar(
                                // A translucent bar lets the list frost through; without blur it
                                // stays the plain opaque surface.
                                color = if (navBlurEnabled) {
                                    MiuixTheme.colorScheme.surface.copy(alpha = CapsuleSurfaceAlpha)
                                } else {
                                    MiuixTheme.colorScheme.surface
                                },
                                modifier = if (navBlurEnabled) {
                                    Modifier
                                        .drawWithContent {
                                            if (barBlurRefresh.value < 0f) {
                                                return@drawWithContent
                                            }
                                            drawContent()
                                        }
                                        .drawBackdrop(
                                            backdrop = activeNavBackdrop,
                                            shape = { RectangleShape },
                                            effects = { blur(NavBlurRadius, NavBlurRadius) }
                                        )
                                } else {
                                    Modifier
                                }
                            ) {
                                visibleDestinations.forEach { destination ->
                                    MiuixNavigationBarItem(
                                        selected = currentDestination == destination,
                                        onClick = { handleDestinationClick(destination) },
                                        icon = destination.navIcon(currentDestination == destination),
                                        label = stringResource(destination.labelRes)
                                    )
                                }
                            }
                        } else {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                visibleDestinations.forEach { destination ->
                                    NavigationBarItem(
                                        selected = currentDestination == destination,
                                        onClick = { handleDestinationClick(destination) },
                                        icon = {
                                            NavigationIcon(
                                                destination, currentDestination == destination
                                            )
                                        },
                                        label = { Text(stringResource(destination.labelRes)) })
                                }
                            }
                        }
                    }
                }
                }
            }
        }
        }
    }

    // Conflict resolution dialog (server version changed while offline)
    uiState.conflict?.let { conflict ->
        ConflictDialog(
            conflict = conflict,
            onResolve = { resolution, mergedContent ->
                viewModel.resolveConflict(resolution, mergedContent)
            },
            onDismiss = { viewModel.dismissConflict() }
        )
    }

}

/** Miuix navigation items take a vector icon, so the avatar stays on the profile page itself. */
internal fun MainDestination.navIcon(selected: Boolean): ImageVector = when (this) {
    MainDestination.MAP -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
    MainDestination.MEMOS ->
        if (selected) Icons.AutoMirrored.Filled.LibraryBooks else Icons.AutoMirrored.Outlined.LibraryBooks
    MainDestination.ATTACHMENTS -> if (selected) Icons.Filled.Attachment else Icons.Outlined.Attachment
    MainDestination.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}

/** Blur radius of the capsule's compose button, in pixels. */
private const val CapsuleBlurRadius = 18f

/** Thickness of the crisp specular line that outlines the capsule. */

/**
 * Width of the soft inner rim just inside that line. The reference material has a ~2dp bevel,
 * brightest along the top edge and fading into the surface.
 */

/** Spread of the soft ambient shadow that lifts the capsule off the page. */
private val CapsuleShadowSpread = 34.dp

/** The shadow is nudged down so it reads as a light source above the bar. */
private val CapsuleShadowOffsetY = 4.dp

/** The capsule's own height; the shadow's corner radius is derived from it. */
private val CapsuleBarHeight = 64.dp

/**
 * Gap between the capsule and the navigation-bar inset. The system gesture handle lives inside
 * that inset and must stay free of the bar's background (and of its shadow).
 */
private val CapsuleNavigationGap = 7.dp

/**
 * Steps in the stacked shadow. Kept moderate: with dozens of very faint layers the per-layer
 * alpha can round away during blending (which made the 3% version disappear), while too many
 * 1-LSB steps build a shadow much stronger than the requested total. Sixteen steps land near the
 * reference's edge shading and still render reliably.
 */
private const val CapsuleShadowLayers = 16

/** The bar's capsule shape, reused by its items so the press feedback follows the bar's corners. */
private val CapsuleShape = RoundedCornerShape(percent = 50)

/**
 * Slot the compose button occupies in the capsule. It is wider than the blue squircle so that a
 * tap right next to the button still belongs to the bar instead of the content behind it.
 */
private val CapsuleButtonSlotWidth = 68.dp

/**
 * Length of the floating capsule with four destinations enabled. The bar keeps this length when
 * fewer destinations are on, so the navigation does not change shape when items are toggled.
 */
private val CapsuleBarWidth = 272.dp

/** The shared enter/exit for the capsule and its shadow, so both settle on the same frame. */
private val capsuleEnter: EnterTransition =
    fadeIn(animationSpec = tween(180)) + scaleIn(
        animationSpec = tween(220),
        initialScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, 1f)
    )

private val capsuleExit: ExitTransition =
    fadeOut(animationSpec = tween(160)) + scaleOut(
        animationSpec = tween(180),
        targetScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, 1f)
    )

/**
 * The shadow arrives on a shorter fade than the bar: 5% black only becomes visible late in a
 * 180ms fade, so matching the bar's curve made the halo read as a separate, later event.
 */
private val capsuleShadowEnter: EnterTransition =
    fadeIn(animationSpec = tween(120)) + scaleIn(
        animationSpec = tween(220),
        initialScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, 1f)
    )

private val capsuleShadowExit: ExitTransition =
    fadeOut(animationSpec = tween(120)) + scaleOut(
        animationSpec = tween(180),
        targetScale = 0.92f,
        transformOrigin = TransformOrigin(0.5f, 1f)
    )

/**
 * The floating bottom bar: a capsule that hovers over the content with the compose button in the
 * middle, so the primary action lives in the navigation instead of a separate floating button.
 */
@Composable
private fun FloatingBottomBar(
    visible: Boolean,
    destinations: List<MainDestination>,
    current: MainDestination,
    onDestination: (MainDestination) -> Unit,
    onCompose: () -> Unit,
    blurEnabled: Boolean,
    backdrop: LayerBackdrop,
    blurRefresh: State<Float>,
    modifier: Modifier = Modifier
) {
    // Read the refresh state in composition, not only in draw: the drawBackdrop modifier element
    // must be updated while the destination cross-fade runs, or its internal blurred layer stays
    // cached and the bar keeps showing the previous page until some unrelated invalidation.
    val refreshTick = blurRefresh.value
    // The capsule is the card color, not the page surface: white in the day theme and #242424 in
    // the dark one, matching the reference. With blur on, that same color goes translucent so the
    // feed still frosts through; the same alpha in both themes keeps the material identical.
    val lightSurface = MiuixTheme.colorScheme.surface.luminance() > 0.5f
    val barColor = MiuixTheme.colorScheme.surfaceContainer.copy(
        alpha = if (blurEnabled) CapsuleSurfaceAlpha else 1f
    )
    // The bevel and its glow live in ui/theme/CapsuleMaterial so the composer, the detail page and
    // the docked FAB can wear the same glass without copying the gradients.
    Box(
        modifier = modifier
            .padding(horizontal = 20.dp)
            // The gesture handle belongs to the navigation-bar inset. Without this the capsule
            // sits on top of the handle and the system's swipe-up area runs through the bar.
            .navigationBarsPadding()
            .padding(top = CapsuleNavigationGap, bottom = CapsuleNavigationGap)
    ) {
        // The shadow gets its own enter/exit layer instead of sitting under the bar's fade: at 5%
        // black it crosses the visibility threshold only late in a 0-1 fade, which read as the
        // halo arriving a beat after the capsule. As a sibling on the same timeline the halo is
        // at full strength halfway through the fade instead of a quarter of it.
        CapsuleShadowLayer(visible = visible, lightSurface = lightSurface)
        AnimatedVisibility(
            visible = visible,
            enter = capsuleEnter,
            exit = capsuleExit
        ) {
        Row(
            modifier = Modifier
                // A fixed length keeps the capsule the same width whatever destination count is
                // enabled; the two halves split it around the centred compose button.
                .width(CapsuleBarWidth)
                .drawWithContent {
                    // Resample the backdrop while the destination cross-fade runs. Without this
                    // read the bar is not invalidated by content changes, so its blur would keep
                    // showing the page that was just switched away from.
                    if (refreshTick < 0f || blurRefresh.value < 0f) {
                        return@drawWithContent
                    }
                    drawContent()
                }
                    .clip(CapsuleShape)
                    .then(
                        if (blurEnabled) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RectangleShape },
                                effects = { blur(NavBlurRadius, NavBlurRadius) }
                            )
                        } else {
                            Modifier
                        }
                    )
                    .background(barColor)
                    // The rim is drawn after the fill so it stays crisp while pressed.
                    .capsuleHighlight(lightSurface = lightSurface)
                    // The capsule floats over the feed, so a tap on the parts of it that are not
                    // a destination must not reach the memo underneath. The destinations are
                    // children of this row and keep their own clicks; this only makes the bar
                    // itself a hit target.
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false).consume()
                        }
                    }
                    .height(CapsuleBarHeight)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val firstHalf = (destinations.size + 1) / 2
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    destinations.take(firstHalf).forEach { destination ->
                        FloatingBarItem(
                            destination = destination,
                            selected = current == destination,
                            onClick = { onDestination(destination) },
                            // The item takes the whole slot, so with two destinations a tap
                            // anywhere in that half of the bar reaches it.
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                ComposeCapsuleButton(
                    onClick = onCompose,
                    blurEnabled = blurEnabled,
                    backdrop = backdrop
                )
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    destinations.drop(firstHalf).forEach { destination ->
                        FloatingBarItem(
                            destination = destination,
                            selected = current == destination,
                            onClick = { onDestination(destination) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * The capsule's ambient shadow: a stack of faint rounded layers on the bar's enter/exit timeline,
 * but in a layer of its own so the bar's 0-1 fade does not dim the 5% black behind it.
 */
@Composable
private fun CapsuleShadowLayer(
    visible: Boolean,
    lightSurface: Boolean
) {
    AnimatedVisibility(
        visible = visible,
        enter = capsuleShadowEnter,
        exit = capsuleShadowExit
    ) {
        Box(
            modifier = Modifier
                .width(CapsuleBarWidth)
                .height(CapsuleBarHeight)
                .drawBehind {
                    // Day keeps a soft ambient shadow; night gets none, because the reference bar
                    // is separated from black by its rim and a dark halo over gray cards was the
                    // effect to avoid.
                    val shadowMaxAlpha = if (lightSurface) 0.05f else 0f
                    if (shadowMaxAlpha <= 0f || size.height <= 0f) return@drawBehind
                    // Each stacked capsule is faint; together they reach `shadowMaxAlpha` at the
                    // bar and fade to nothing at the edge of the spread.
                    val layerAlpha = 1f - (1f - shadowMaxAlpha).pow(1f / CapsuleShadowLayers)
                    val spread = CapsuleShadowSpread.toPx()
                    val offsetY = CapsuleShadowOffsetY.toPx()
                    // Fixed bar height: a first draw before layout would otherwise produce a
                    // square-cornered block instead of a capsule.
                    val radius = CapsuleBarHeight.toPx() / 2f
                    val step = spread / CapsuleShadowLayers
                    for (i in CapsuleShadowLayers downTo 1) {
                        val grow = step * i
                        drawRoundRect(
                            color = Color.Black.copy(alpha = layerAlpha),
                            topLeft = Offset(-grow, -grow + offsetY),
                            size = Size(size.width + grow * 2f, size.height + grow * 2f),
                            cornerRadius = CornerRadius(radius + grow)
                        )
                    }
                }
        )
    }
}

@Composable
private fun FloatingBarItem(
    destination: MainDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    }
    // Icon only: a floating bar is narrower than the docked one, and the labels made the icons
    // read as cramped. The destination name stays available to accessibility services.
    // The press tint uses the capsule shape as well, so its corners match the bar's instead of
    // showing a smaller rounded rectangle inside the capsule.
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(CapsuleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = destination.navIcon(selected),
            contentDescription = stringResource(destination.labelRes),
            tint = color,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun ComposeCapsuleButton(
    onClick: () -> Unit,
    blurEnabled: Boolean,
    backdrop: LayerBackdrop
) {
    // Squircle rather than a circle, so it reads as the capsule's own action. Two thirds of the
    // old height: the corner radius shrinks with it, otherwise it would clamp into a pill.
    val squircle = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(CapsuleButtonSlotWidth),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(35.dp)
                .clip(squircle)
                .then(
                    if (blurEnabled) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RectangleShape },
                            effects = { blur(CapsuleBlurRadius, CapsuleBlurRadius) }
                        )
                    } else {
                        Modifier
                    }
                )
                .background(
                    MiuixTheme.colorScheme.primary.copy(alpha = if (blurEnabled) 0.78f else 1f)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.memo_composer_fab_new_memo),
                tint = MiuixTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
