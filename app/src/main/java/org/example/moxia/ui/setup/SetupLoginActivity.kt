package org.example.moxia.ui.setup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import org.example.moxia.ui.theme.AppCircularProgressIndicator
import org.example.moxia.ui.theme.AppSnackbarHost
import org.example.moxia.ui.theme.AppTopBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.example.moxia.R
import org.example.moxia.data.DataStoreManager
import org.example.moxia.data.backup.BackupCoordinator
import org.example.moxia.ui.component.LocalNetworkPermission
import org.example.moxia.ui.component.LoginContent
import org.example.moxia.ui.component.rememberLocalNetworkPermission
import org.example.moxia.ui.theme.SavedMoxiaTheme
import org.example.moxia.ui.theme.navigationBarBottom
import org.example.moxia.ui.theme.pageContentWindowInsets
import org.koin.android.ext.android.inject

class SetupLoginActivity : ComponentActivity() {
    private val settings: DataStoreManager by inject()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val fromSettings = intent.getBooleanExtra(FROM_SETTINGS, false)
        setContent {
            SavedMoxiaTheme(settings) {
                val ready by BackupCoordinator.startupReady.collectAsState()
                val permission = rememberLocalNetworkPermission()
                val scope = rememberCoroutineScope()
                val snackbar = remember { SnackbarHostState() }
                val saveError = stringResource(R.string.login_error_failed)
                var saving by remember { mutableStateOf(false) }
                BackHandler(enabled = saving) {}
                CompositionLocalProvider(LocalNetworkPermission provides permission) {
                    Scaffold(
                        topBar = {
                            AppTopBar(
                                title = "",
                                navigationIcon = {
                                    IconButton(enabled = !saving, onClick = { onBackPressedDispatcher.onBackPressed() }) {
                                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.backup_back))
                                    }
                                },
                                actions = {
                                    if (fromSettings) IconButton(enabled = !saving, onClick = {
                                        setResult(RESULT_CLOSE_SETUP)
                                        finish()
                                    }) {
                                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_close))
                                    }
                                }
                            )
                        },
                        snackbarHost = { AppSnackbarHost(snackbar) },
                        // The form runs under the system bars; only its own padding keeps the
                        // status bar and the gesture handle clear.
                        contentWindowInsets = pageContentWindowInsets()
                    ) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
                            if (!ready || saving) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    AppCircularProgressIndicator()
                                }
                            } else {
                                LoginContent(
                                    modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()
                                        .padding(24.dp)
                                        .padding(bottom = navigationBarBottom()),
                                    onLoginSuccess = { baseUrl, token ->
                                        scope.launch {
                                            saving = true
                                            android.util.Log.d("MemosLogin", "addAccount: start")
                                            try {
                                                settings.addAccount(baseUrl, token)
                                                android.util.Log.d("MemosLogin", "addAccount: done")
                                                setResult(RESULT_OK)
                                                finish()
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: Exception) {
                                                android.util.Log.e("MemosLogin", "addAccount failed", error)
                                                saving = false
                                                snackbar.showSnackbar(saveError)
                                            } finally {
                                                saving = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val FROM_SETTINGS = "from_settings"
        const val RESULT_CLOSE_SETUP = RESULT_FIRST_USER
        fun createIntent(context: Context, fromSettings: Boolean = false): Intent =
            Intent(context, SetupLoginActivity::class.java).putExtra(FROM_SETTINGS, fromSettings)
    }
}
