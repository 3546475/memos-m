package org.example.memosm.ui

import org.koin.core.context.GlobalContext

import org.example.memosm.data.DataStoreManager

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

import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin


import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import org.koin.androidx.compose.koinViewModel
import kotlinx.coroutines.launch
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

@Composable
fun MainScreen(
    onLogout: () -> Unit,
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
    val viewModel: MemosViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val previewRepository by viewModel.linkPreviews.collectAsState()
    val linkPreviews = remember(previewRepository, uiState.appSettings.linkPreviewEnabled, uiState.isOnline) {
        previewRepository?.takeIf { uiState.appSettings.linkPreviewEnabled }
            ?.let { LinkPreviewEnvironment(it, uiState.isOnline) }
    }
    val mediaIdentity = uiState.accounts.firstOrNull { it.isActive }?.let {
        AccountMediaIdentity(it.id, uiState.accountGeneration)
    }
    CompositionLocalProvider(
        LocalAccountMediaIdentity provides mediaIdentity,
        LocalLinkPreviews provides linkPreviews
    ) {
        key(uiState.accountGeneration) {
            MainScreenContent(viewModel, uiState, destination, onLogout, modifier,
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
    val mapSupported = uiState.memoMap.capability == MapCapability.SUPPORTED
    val dataStore = remember { GlobalContext.get().get<DataStoreManager>() }
    val appearance by dataStore.appearance.collectAsState(initial = AppearancePreferences())
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

    val adaptiveInfo = androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2()


    // Non-null means "this layout has a bottom bar to hide". Screens use it both to hide the bar on
    // scroll and to reserve bottom padding for it, so with the bar pinned visible the callback must
    // still exist — it just ignores the scroll updates.
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

    // Frost the content behind the bars on API 33+ in MIUIX only, and only while the switch is on.
    val navBackdrop = rememberLayerBackdrop()

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
                        .then(
                            if (navBlurEnabled) Modifier.layerBackdrop(navBackdrop) else Modifier
                        )
                ) {
                    // Content
                    val miuixMotion = isMiuixMotion(LocalThemeStyle.current)
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = {
                            val spec = appFadeSpec(miuixMotion)
                            fadeIn(animationSpec = spec) togetherWith fadeOut(animationSpec = spec)
                        },
                        label = "MainScreenDestinationTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { targetDestination ->
                        val destinationKey = if (targetDestination == MainDestination.MAP) {
                            "map:${uiState.accounts.firstOrNull { it.isActive }?.id}:${uiState.session.hostUrl}"
                        } else targetDestination
                        saveableStateHolder.SaveableStateProvider(destinationKey) {
                            when (targetDestination) {
                                MainDestination.MAP -> if (mapSupported) {
                                    key(uiState.accountGeneration) {
                                        MapScreen(viewModel, isNavBarVisible = isNavBarVisible, onToggleNavBar = toggleNavBar)
                                    }
                                }
                                MainDestination.MEMOS -> MemosScreen(
                                    viewModel = viewModel,
                                    pagerState = feedPagerState,
                                    topBarScrollMode = appearance.topBarScrollMode,
                                    onToggleNavBar = toggleNavBar,
                                    isNavBarVisible = isNavBarVisible,
                                    // The floating bar carries the compose action already.
                                    showComposeFab = !(
                                        LocalThemeStyle.current == ThemeStyle.MIUIX &&
                                            appearance.floatingNavBar
                                        ),
                                    openComposer = shouldOpenComposer,
                                    onComposerOpened = onComposerOpened,
                                    openSearch = openSearchRequest,
                                    onSearchOpened = onOpenSearchRequestConsumed
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
                                    isNavBarVisible = isNavBarVisible
                                )
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
                        visible = isNavBarVisible,
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
                        backdrop = navBackdrop,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                } else {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    AnimatedVisibility(
                        visible = isNavBarVisible,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        if (LocalThemeStyle.current == ThemeStyle.MIUIX) {
                            MiuixNavigationBar(
                                // A translucent bar lets the list frost through; without blur it
                                // stays the plain opaque surface.
                                color = if (navBlurEnabled) {
                                    MiuixTheme.colorScheme.surface.copy(alpha = 0.72f)
                                } else {
                                    MiuixTheme.colorScheme.surface
                                },
                                modifier = if (navBlurEnabled) {
                                    Modifier.drawBackdrop(
                                        backdrop = navBackdrop,
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
private fun MainDestination.navIcon(selected: Boolean): ImageVector = when (this) {
    MainDestination.MAP -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
    MainDestination.MEMOS ->
        if (selected) Icons.AutoMirrored.Filled.LibraryBooks else Icons.AutoMirrored.Outlined.LibraryBooks
    MainDestination.ATTACHMENTS -> if (selected) Icons.Filled.Attachment else Icons.Outlined.Attachment
    MainDestination.PROFILE -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}

/** Blur radius of the capsule's compose button, in pixels. */
private const val CapsuleBlurRadius = 18f

/**
 * Length of the floating capsule with four destinations enabled. The bar keeps this length when
 * fewer destinations are on, so the navigation does not change shape when items are toggled.
 */
private val CapsuleBarWidth = 272.dp

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
    modifier: Modifier = Modifier
) {
    val capsule = RoundedCornerShape(percent = 50)
    Box(modifier = modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        AnimatedVisibility(
            visible = visible,
            // Scale and fade instead of a full-height slide: the capsule stays inside the screen
            // for the whole animation, so its drop shadow is never cut off at the edge and cannot
            // pop in a beat after the bar itself (which read as a stutter).
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(
                animationSpec = tween(220),
                initialScale = 0.92f,
                transformOrigin = TransformOrigin(0.5f, 1f)
            ),
            exit = fadeOut(animationSpec = tween(160)) + scaleOut(
                animationSpec = tween(180),
                targetScale = 0.92f,
                transformOrigin = TransformOrigin(0.5f, 1f)
            )
        ) {
            Row(
                modifier = Modifier
                    // A fixed length keeps the capsule the same width whatever destination count is
                    // enabled; the two halves split it around the centred compose button.
                    .width(CapsuleBarWidth)
                    .shadow(elevation = 10.dp, shape = capsule, clip = false)
                    .clip(capsule)
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
                    .background(
                        if (blurEnabled) {
                            MiuixTheme.colorScheme.surface.copy(alpha = 0.72f)
                        } else {
                            MiuixTheme.colorScheme.surface
                        }
                    )
                    .height(58.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val firstHalf = (destinations.size + 1) / 2
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    destinations.take(firstHalf).forEach { destination ->
                        FloatingBarItem(
                            destination = destination,
                            selected = current == destination,
                            onClick = { onDestination(destination) }
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
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    destinations.drop(firstHalf).forEach { destination ->
                        FloatingBarItem(
                            destination = destination,
                            selected = current == destination,
                            onClick = { onDestination(destination) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingBarItem(
    destination: MainDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.onSurfaceVariantActions
    }
    Column(
        modifier = Modifier
            .width(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = destination.navIcon(selected),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(destination.labelRes),
            color = color,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ComposeCapsuleButton(
    onClick: () -> Unit,
    blurEnabled: Boolean,
    backdrop: LayerBackdrop
) {
    // Squircle rather than a circle, so it reads as the capsule's own action.
    val squircle = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(52.dp)
            .shadow(elevation = 6.dp, shape = squircle, clip = false)
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
