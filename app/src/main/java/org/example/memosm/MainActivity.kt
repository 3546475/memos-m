package org.example.memosm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.example.memosm.data.DataStoreManager
import org.example.memosm.model.ShareIntentData
import org.example.memosm.ui.MainScreen
import org.example.memosm.ui.MainDestination
import org.example.memosm.ui.profile.DraftsActivity
import org.example.memosm.ui.profile.NotificationsActivity
import org.example.memosm.ui.component.LoginScreen
import org.example.memosm.ui.component.LocalNetworkPermission
import org.example.memosm.ui.component.rememberLocalNetworkPermission
import org.example.memosm.ui.theme.AppCircularProgressIndicator
import org.example.memosm.ui.theme.SavedMemosMTheme
import org.example.memosm.ui.setup.SetupActivity
import org.example.memosm.viewmodel.MemosViewModel
import org.example.memosm.widget.DraftWidget

import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val dataStoreManager: DataStoreManager by inject()
    private val viewModel: MemosViewModel by inject()

    // StateFlow to hold pending share data, observable by Compose
    private val pendingShareDataFlow = MutableStateFlow<ShareIntentData?>(null)

    // StateFlow to trigger composer opening from widget
    private val shouldOpenComposerFlow = MutableStateFlow(false)

    // One-shot requests raised by launcher shortcuts and widgets that must survive the
    // login/inspection gate before the main screen can consume them.
    private val pendingDestinationFlow = MutableStateFlow<MainDestination?>(null)
    private val pendingSearchFlow = MutableStateFlow(false)
    private val pendingDraftsFlow = MutableStateFlow(false)
    private val pendingNotificationsFlow = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Parse share intent data from initial launch
        pendingShareDataFlow.value = parseShareIntent(intent)

        // Check if launched from widget
        if (intent.action == DraftWidget.ACTION_OPEN_COMPOSER) {
            shouldOpenComposerFlow.value = true
        }
        handleEntryIntent(intent)

        setContent {
            SavedMemosMTheme(dataStoreManager) {
                val networkPermission = rememberLocalNetworkPermission()
                CompositionLocalProvider(LocalNetworkPermission provides networkPermission) {
                    val scope = rememberCoroutineScope()

                    val recoveryError by org.example.memosm.data.backup.BackupCoordinator.recoveryError.collectAsState()
                    val startupReady by org.example.memosm.data.backup.BackupCoordinator.startupReady.collectAsState()
                    if (!startupReady) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AppCircularProgressIndicator()
                        }
                    } else if (recoveryError != null) {
                        androidx.compose.foundation.layout.Column(
                            Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.material3.Text(recoveryError!!)
                            androidx.compose.material3.Button(onClick = {
                                scope.launch {
                                    val backup = org.koin.core.context.GlobalContext.get().get<org.example.memosm.data.backup.BackupService>()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                                        backup.recoverInterruptedRestore().onSuccess {
                                            backup.restoreNativeIfPresent().onFailure {
                                                org.example.memosm.data.backup.BackupCoordinator.recoveryError.value = getString(R.string.backup_native_recovery_failed)
                                            }
                                        }
                                    }
                                }
                            }) { androidx.compose.material3.Text(getString(R.string.backup_recovery_retry)) }
                        }
                    } else {
                    // Observe accounts instead of single credentials
                    val accounts by dataStoreManager.accounts.collectAsState(initial = null)
                    val setupCompleted by dataStoreManager.setupCompleted.collectAsState(initial = null)
                    // Wait for the saved appearance before composing the main screen: the
                    // default preferences would otherwise show the docked bar first and then
                    // switch to the saved floating bar.
                    val appearance by dataStoreManager.appearance.collectAsState(initial = null)
                    var setupLaunched by rememberSaveable { mutableStateOf(false) }
                    val setupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        if (it.resultCode == RESULT_OK) {
                            viewModel.userDelegate.updateCurrentAccountInList()
                        } else {
                            // Back from first-launch setup leaves the app; the next launch retries.
                            finish()
                        }
                    }

                    // Wait for DataStore to emit initial values
                    var isCheckingSession by remember { mutableStateOf(true) }

                    // Collect pending share data from the flow
                    val pendingShareData by pendingShareDataFlow.collectAsState()
                    val shouldOpenComposer by shouldOpenComposerFlow.collectAsState()
                    val pendingDestination by pendingDestinationFlow.collectAsState()
                    val pendingSearch by pendingSearchFlow.collectAsState()
                    val pendingDrafts by pendingDraftsFlow.collectAsState()
                    val pendingNotifications by pendingNotificationsFlow.collectAsState()

                    LaunchedEffect(accounts, setupCompleted) {
                        if (accounts != null && setupCompleted != null) {
                            // Once we have a non-null list (even if empty), we've finished the initial load
                            isCheckingSession = false
                            if (setupCompleted == false) {
                                if (accounts!!.isNotEmpty()) {
                                    // Existing installations stay onboarded even after logout.
                                    dataStoreManager.completeSetup()
                                } else if (!setupLaunched) {
                                    setupLaunched = true
                                    setupLauncher.launch(SetupActivity.createIntent(this@MainActivity))
                                }
                            }
                        }
                    }

                    if (isCheckingSession || setupCompleted != true || appearance == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            AppCircularProgressIndicator()
                        }
                    } else {
                        val activeAccount = accounts?.find { it.isActive }
                        var checkedAccount by remember { mutableStateOf<String?>(null) }
                        LaunchedEffect(activeAccount?.id) {
                            activeAccount?.let {
                                networkPermission.ensureAccess(it.hostUrl)
                                checkedAccount = it.id
                            }
                        }

                        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                            if (activeAccount != null && checkedAccount != activeAccount.id) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    AppCircularProgressIndicator()
                                }
                            } else if (activeAccount != null) {
                                // Launcher shortcuts land on the page they name; child pages that
                                // own their own window are opened once the session is bound.
                                LaunchedEffect(pendingDrafts) {
                                    if (pendingDrafts) {
                                        pendingDraftsFlow.value = false
                                        startActivity(
                                            Intent(this@MainActivity, DraftsActivity::class.java)
                                        )
                                    }
                                }
                                LaunchedEffect(pendingNotifications) {
                                    if (pendingNotifications) {
                                        pendingNotificationsFlow.value = false
                                        startActivity(
                                            Intent(this@MainActivity, NotificationsActivity::class.java)
                                        )
                                    }
                                }
                                MainScreen(
                                    appearance = appearance!!,
                                    onLogout = {
                                        scope.launch {
                                            viewModel.userDelegate.removeAccount(activeAccount)
                                        }
                                    },
                                    shareIntentData = pendingShareData,
                                    onShareIntentConsumed = { pendingShareDataFlow.value = null },
                                    shouldOpenComposer = shouldOpenComposer,
                                    onComposerOpened = { shouldOpenComposerFlow.value = false },
                                    destinationRequest = pendingDestination,
                                    onDestinationRequestConsumed = { pendingDestinationFlow.value = null },
                                    openSearchRequest = pendingSearch,
                                    onOpenSearchRequestConsumed = { pendingSearchFlow.value = false })
                            } else {
                                // If no active account, show login
                                LoginScreen(
                                modifier = Modifier.padding(innerPadding),
                                onLoginSuccess = { baseUrl, token ->
                                    Log.d("MemosLogin", "main login: handler entered")
                                    scope.launch {
                                        Log.d(
                                            "MemosLogin",
                                            "main login: ready=${org.example.memosm.data.backup.BackupCoordinator.startupReady.value} " +
                                                "restoring=${org.example.memosm.data.backup.BackupCoordinator.restoring.value} " +
                                                "recoveryError=${org.example.memosm.data.backup.BackupCoordinator.recoveryError.value}"
                                        )
                                        try {
                                            // A stuck storage lock used to leave the login form on
                                            // screen with no feedback; surface it instead.
                                            val saved = kotlinx.coroutines.withTimeoutOrNull(15_000) {
                                                dataStoreManager.addAccount(baseUrl, token)
                                                true
                                            }
                                            Log.d("MemosLogin", "main login: addAccount returned saved=$saved")
                                            if (saved == null) {
                                                android.widget.Toast.makeText(
                                                    this@MainActivity,
                                                    R.string.login_error_failed,
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                            // Rebind the API/session immediately so the offline
                                            // cache and sync stack use the newly added account.
                                            viewModel.userDelegate.updateCurrentAccountInList()
                                            Log.d("MemosLogin", "main login: rebound")
                                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                            throw cancelled
                                        } catch (error: Exception) {
                                            Log.e("MemosLogin", "main login: failed", error)
                                            android.widget.Toast.makeText(
                                                this@MainActivity,
                                                R.string.login_error_failed,
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                })
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // App is in the foreground again: flush queued offline writes and
        // top up the local cache.
        if (org.example.memosm.data.backup.BackupCoordinator.recoveryError.value == null) viewModel.onForeground()
    }

    /**
     * Called when the activity is already running and receives a new intent (e.g., share).
     * With launchMode="singleTask", share intents will come here instead of onCreate.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Update the current intent

        // Parse and emit the new share data
        val shareData = parseShareIntent(intent)
        if (shareData != null) {
            pendingShareDataFlow.value = shareData
        }

        if (intent.action == DraftWidget.ACTION_OPEN_COMPOSER) {
            shouldOpenComposerFlow.value = true
        }
        handleEntryIntent(intent)
    }

    /**
     * Routes the launcher shortcut and widget actions that name a page. The composer request is
     * kept on its own flag because [MainScreen] already consumes it after switching feeds.
     *
     * The markers are cleared from the retained intent afterwards: this activity is recreated on
     * a configuration change, and replaying "open the drafts" would reopen the page the user had
     * already left.
     */
    private fun handleEntryIntent(intent: Intent?) {
        if (intent == null) return
        var handled = false
        when (intent.action) {
            AppShortcuts.ACTION_NEW_MEMO -> {
                shouldOpenComposerFlow.value = true
                handled = true
            }
            AppShortcuts.ACTION_OPEN_DRAFTS -> {
                pendingDraftsFlow.value = true
                handled = true
            }
            AppShortcuts.ACTION_OPEN_NOTIFICATIONS -> {
                pendingNotificationsFlow.value = true
                handled = true
            }
            AppShortcuts.ACTION_OPEN_SEARCH -> {
                pendingSearchFlow.value = true
                handled = true
            }
            else -> Unit
        }
        intent.getStringExtra(EXTRA_DESTINATION)
            ?.let { name -> MainDestination.entries.firstOrNull { it.name == name } }
            ?.let {
                pendingDestinationFlow.value = it
                handled = true
            }
        if (handled) {
            intent.removeExtra(EXTRA_DESTINATION)
            val action = intent.action
            if (action != null && !action.startsWith("android.intent.action.")) intent.action = null
        }
    }

    companion object {
        /** Tab a widget should land on, stored as the [MainDestination] name. */
        const val EXTRA_DESTINATION = "org.example.memosm.extra.DESTINATION"
    }

    /**
     * Parses a share intent (ACTION_SEND or ACTION_SEND_MULTIPLE) and extracts
     * text content and file URIs.
     */
    private fun parseShareIntent(intent: Intent?): ShareIntentData? {
        if (intent == null) return null

        val action = intent.action
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return null
        }

        Log.d("MainActivity", "parseShareIntent: action=$action")

        // Extract text content
        val text =
            intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
        Log.d("MainActivity", "parseShareIntent: text=${text?.take(100)}")

        // Extract URIs
        val uris = mutableListOf<Uri>()

        when (action) {
            Intent.ACTION_SEND -> {
                // Single file/image share
                val singleUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                Log.d("MainActivity", "parseShareIntent: SEND singleUri=$singleUri")
                singleUri?.let { uri ->
                    val localUri = copyUriToCache(uri)
                    Log.d("MainActivity", "parseShareIntent: copied to localUri=$localUri")
                    if (localUri != null) uris.add(localUri)
                }
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                // Multiple files/images share
                val multipleUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                Log.d("MainActivity", "parseShareIntent: SEND_MULTIPLE count=${multipleUris?.size}")
                multipleUris?.forEach { uri ->
                    val localUri = copyUriToCache(uri)
                    Log.d("MainActivity", "parseShareIntent: copied $uri -> $localUri")
                    if (localUri != null) uris.add(localUri)
                }
            }
        }

        val shareData = ShareIntentData(text = text, uris = uris)
        Log.d(
            "MainActivity",
            "parseShareIntent: result isEmpty=${shareData.isEmpty}, uriCount=${uris.size}"
        )
        return if (shareData.isEmpty) null else shareData
    }

    /**
     * Copies a shared URI's content to a local cache file so it remains
     * accessible after the temporary share-intent permission expires.
     * Preserves the original filename/extension for proper MIME type detection.
     * Returns the local file Uri, or null if the copy fails.
     */
    private fun copyUriToCache(uri: Uri): Uri? {
        return try {
            // Try to get the original display name from ContentResolver
            var displayName: String? = null
            if (uri.scheme == "content") {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index =
                            cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) displayName = cursor.getString(index)
                    }
                }
            }

            // Fallback: derive filename from URI path or MIME type
            if (displayName.isNullOrBlank()) {
                val baseName = uri.lastPathSegment ?: getString(R.string.attachments_unknown_filename)
                val mimeType = contentResolver.getType(uri)
                val ext = if (mimeType != null) {
                    android.webkit.MimeTypeMap.getSingleton()
                        .getExtensionFromMimeType(mimeType)
                } else null
                displayName = if (ext != null && !baseName.contains('.')) {
                    "$baseName.$ext"
                } else {
                    baseName
                }
            }

            val fileName = "share_${System.currentTimeMillis()}_$displayName"
            val cacheFile = java.io.File(cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                Log.w("MainActivity", "Could not open input stream for shared URI: $uri")
                return null
            }
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            Log.w("MainActivity", "Failed to copy shared URI to cache: $uri", e)
            null
        }
    }
}
