package org.example.moxia.ui.nav

import AccountsList
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import org.example.moxia.ui.profile.EditProfileActivity
import org.example.moxia.ui.profile.MemoTypeMemosActivity
import org.example.moxia.ui.profile.EditAccountCredentialsActivity
import org.example.moxia.ui.profile.ArchivedMemosActivity
import org.example.moxia.ui.profile.DraftsActivity
import org.example.moxia.ui.profile.NotificationsActivity
import org.example.moxia.ui.profile.SettingsActivity
import org.example.moxia.ui.theme.ProfileTheme
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.example.moxia.ui.component.ActionSheet
import org.example.moxia.ui.component.LocalNetworkPermission
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.moxia.R
import org.example.moxia.data.DataStoreManager
import org.example.moxia.model.InstanceProfile
import org.example.moxia.model.MemoContentType
import org.example.moxia.model.Shortcut
import org.example.moxia.model.UserSnapshot
import org.example.moxia.model.toUserSnapshot
import org.example.moxia.ui.component.ErrorView
import org.example.moxia.ui.component.ProfileHeader
import org.example.moxia.ui.component.ProfileStatsRow
import org.example.moxia.ui.component.TagTreeDialog
import org.example.moxia.ui.component.rememberLocalMemoCatalog
import org.example.moxia.model.matchesContentType
import org.example.moxia.ui.component.StatsActivityCard
import org.example.moxia.ui.component.rememberPageOverscroll
import org.example.moxia.ui.component.rememberScrollContext
import org.example.moxia.ui.component.setting.SettingsGroup
import org.example.moxia.ui.component.setting.SettingsNavigationRow
import org.example.moxia.ui.theme.AppCircularProgressIndicator
import org.example.moxia.ui.theme.AppListItem
import org.example.moxia.ui.theme.AppPullToRefresh
import org.example.moxia.viewmodel.MemosViewModel
import org.example.moxia.viewmodel.RefreshSource
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    viewModel: MemosViewModel,
    onLogout: () -> Unit,
    onAddAccount: () -> Unit,
    onToggleNavBar: ((Boolean) -> Unit)? = null,
    isNavBarVisible: Boolean = true,
    /** Switches the app back to the notes feed, used by the stat card shortcuts. */
    onOpenMemos: () -> Unit = {}
) {
    val context = LocalContext.current
    val archived = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAfterProfileDetails()
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAfterProfileDetails()
    }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAfterProfileDetails()
    }
    val editProfile = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAfterProfileDetails()
    }
    val drafts = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAfterEditor()
    }
    val listState = rememberLazyListState()
    ProfileTheme {
        ProfileListPane(
            viewModel = viewModel,
            onLogout = onLogout,
            onAddAccount = onAddAccount,
            onShowArchived = { archived.launch(Intent(context, ArchivedMemosActivity::class.java)) },
            onShowDrafts = { drafts.launch(Intent(context, DraftsActivity::class.java)) },
            onShowNotifications = { notifications.launch(Intent(context, NotificationsActivity::class.java)) },
            onShowSettings = { settings.launch(Intent(context, SettingsActivity::class.java)) },
            onEditProfile = { editProfile.launch(Intent(context, EditProfileActivity::class.java)) },
            onToggleNavBar = onToggleNavBar,
            isNavBarVisible = isNavBarVisible,
            onOpenMemos = onOpenMemos,
            listState = listState
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileListPane(
    viewModel: MemosViewModel,
    onLogout: () -> Unit,
    onAddAccount: () -> Unit,
    onShowArchived: () -> Unit,
    onShowDrafts: () -> Unit,
    onShowNotifications: () -> Unit,
    onShowSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onToggleNavBar: ((Boolean) -> Unit)? = null,
    isNavBarVisible: Boolean = true,
    onOpenMemos: () -> Unit = {},
    listState: LazyListState
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.session.currUser
    val stats = uiState.session.userStats
    val accounts = uiState.accounts
    val networkPermission = LocalNetworkPermission.current
    val scope = rememberCoroutineScope()
    val catalog = rememberLocalMemoCatalog(viewModel)
    // This page usually fits on one screen and must never rubber-band; see rememberPageOverscroll.
    val overscroll = rememberPageOverscroll()

    // Scroll direction tracking for nav bar visibility
    rememberScrollContext(
        listState = listState,
        onScrollDown = { onToggleNavBar?.invoke(false) },
        onScrollUp = { onToggleNavBar?.invoke(true) })

    val bottomPadding by animateDpAsState(
        targetValue = if (isNavBarVisible) 96.dp else 16.dp, // Profile has more bottom internal padding?
        label = "BottomPadding"
    )

    // Double tap refresh logic: scroll to top
    var lastProcessedTrigger by rememberSaveable { mutableLongStateOf(uiState.refreshTrigger) }
    LaunchedEffect(uiState.refreshTrigger) {
        if (uiState.refreshTrigger > lastProcessedTrigger) {
            if (uiState.refreshSource == RefreshSource.USerMemos || uiState.refreshSource == RefreshSource.Manual) {
                listState.animateScrollToItem(0)
            }
        }
        lastProcessedTrigger = uiState.refreshTrigger
    }

    var showAccountSwitcher by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val dataStore: DataStoreManager = koinInject()
    val editCredentials = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val credentials = EditAccountCredentialsActivity.readResult(result.resultCode, result.data)
        if (credentials != null) {
            scope.launch {
                val account = dataStore.getAccounts().firstOrNull { it.id == credentials.accountId }
                if (account != null) {
                    viewModel.userDelegate.updateAccountCredentials(account, credentials.hostUrl, credentials.token)
                    showAccountSwitcher = false
                }
            }
        }
    }

    // Get current account for profile editing
    val activeAccount = accounts.find { it.isActive }


    if (showAccountSwitcher) {
        ActionSheet(onDismissRequest = { showAccountSwitcher = false }) {
            AccountsList(
                accounts = accounts,
                onSwitchAccount = { account ->
                    scope.launch {
                        if (networkPermission.ensureAccess(account.hostUrl)) {
                            viewModel.userDelegate.switchAccount(account)
                            showAccountSwitcher = false
                        }
                    }
                },
                onActiveAccountClick = {
                    showAccountSwitcher = false
                    onEditProfile()
                },
                onLogoutAccount = { viewModel.userDelegate.removeAccount(it) },
                onEditAccount = { account ->
                    editCredentials.launch(EditAccountCredentialsActivity.createIntent(context, account.id))
                },
                onAddAccount = {
                    onAddAccount()
                    showAccountSwitcher = false
                },
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }

    AppPullToRefresh(
        isRefreshing = uiState.isRefreshing, onRefresh = {
            viewModel.fetchUserMemos(refresh = true)
            viewModel.userDelegate.refreshInstanceSettings()
            viewModel.userDelegate.refreshUserStats()
        }, modifier = Modifier.fillMaxSize(),
        // This page is hosted edge to edge, so the chip has to clear the status bar itself.
        indicatorTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    ) {
        LazyColumn(
            state = listState,
            overscrollEffect = overscroll,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                end = 16.dp,
                // Reserves the floating bar's height (and the navigation area under it). There is
                // deliberately no spacer item after the last row: that only added scroll range
                // through empty space once the page was already fully shown.
                bottom = bottomPadding + WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding()
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val itemModifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()

            item {
                Column(modifier = itemModifier) {
                    val hostUrl = uiState.session.hostUrl
                    if (user != null) {
                        val rawAvatarUrl = user.avatarUrl
                        val avatarUrl = org.example.moxia.ui.component.resolveResourceUrl(
                            hostUrl, rawAvatarUrl
                        )

                        ProfileHeader(
                            user = user.toUserSnapshot(
                                token = uiState.session.token
                            ).copy(avatarUrl = avatarUrl),
                            onClick = { showAccountSwitcher = true })
                    } else {
                        if (activeAccount != null) {
                            val rawAvatarUrl = activeAccount.avatarUrl
                            val avatarUrl = org.example.moxia.ui.component.resolveResourceUrl(
                                hostUrl, rawAvatarUrl
                            )

                            ProfileHeader(
                                user = UserSnapshot(
                                    name = activeAccount.name?.let { "users/$it" },
                                    username = activeAccount.name ?: "",
                                    displayName = activeAccount.displayName,
                                    avatarUrl = avatarUrl,
                                    token = uiState.session.token
                                ),
                                onClick = { showAccountSwitcher = true })
                        } else if (uiState.userMemoList.list.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppCircularProgressIndicator()
                            }
                        }
                    }
                    // Directly under the identity block, on the page background.
                    if (user != null || accounts.any { it.isActive }) {
                        ProfileStatsRow(
                            userStats = stats,
                            linkCount = catalog.count { it.matchesContentType(MemoContentType.LINK) },
                            codeCount = catalog.count { it.matchesContentType(MemoContentType.CODE) },
                            todoCount = catalog.count { it.matchesContentType(MemoContentType.TODO) },
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (user != null || accounts.any { it.isActive }) {
                item {
                    Box(itemModifier) {
                        StatsActivityCard(
                            userStats = stats,
                            weekStartDayOffset = uiState.session.instanceSettings?.generalSetting?.weekStartDayOffset
                                ?: 0
                        )
                    }
                }

                // Content: the pages reached from here belong to the user's own data. Settings owns
                // its sections, so they are not duplicated as rows here.
                item {
                    Box(itemModifier) {
                        SettingsGroup {
                            SettingsNavigationRow(
                                title = stringResource(R.string.profile_notifications),
                                icon = Icons.Outlined.Notifications,
                                onClick = onShowNotifications
                            )
                            SettingsNavigationRow(
                                title = stringResource(R.string.profile_archived),
                                icon = Icons.Outlined.Archive,
                                onClick = onShowArchived
                            )
                            SettingsNavigationRow(
                                title = stringResource(R.string.drafts_title),
                                icon = Icons.Outlined.Drafts,
                                onClick = onShowDrafts
                            )
                        }
                    }
                }

                item {
                    Box(itemModifier) {
                        SettingsGroup {
                            SettingsNavigationRow(
                                title = stringResource(R.string.settings_title),
                                icon = Icons.Outlined.Settings,
                                onClick = onShowSettings
                            )
                        }
                    }
                }

                if (uiState.error != null) {
                    item {
                        Box(itemModifier) {
                            ErrorView(
                                title = stringResource(R.string.common_error_failed_to_load_profile),
                                message = stringResource(uiState.error!!.resourceId, *uiState.error!!.formatArgs.toTypedArray()),
                                onRetry = { viewModel.fetchUserMemos(refresh = true) })
                        }
                    }
                }
            } else if (!uiState.userMemoList.list.isLoading) {
                item {
                    ErrorView(
                        message = uiState.error?.let { stringResource(it.resourceId, *it.formatArgs.toTypedArray()) }
                            ?: stringResource(R.string.profile_user_info_not_available),
                        onRetry = { viewModel.fetchUserMemos(refresh = true) },
                        modifier = itemModifier.fillParentMaxHeight(0.7f)
                    )
                }
            }
        }
    }
}


@Composable
fun InstanceCard(instance: InstanceProfile) {
    val unknown = stringResource(R.string.memo_unknown_user)
    val mode = instance.mode ?: if (instance.demo == true) "demo" else "prod"
    org.example.moxia.ui.component.setting.SettingsGroup {
        listOf(
            stringResource(R.string.profile_instance_version) to (instance.version ?: unknown),
            stringResource(R.string.profile_instance_mode) to mode,
            stringResource(R.string.profile_instance_url) to (instance.instanceUrl?.takeIf { it.isNotBlank() } ?: unknown)
        ).forEach { (label, value) ->
            AppListItem(
                title = label,
                summary = value,
                modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            )
        }
    }
}


@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
