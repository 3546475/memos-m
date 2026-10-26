package org.example.moxia.ui.component

import org.example.moxia.ui.theme.AppCircularProgressIndicator
import org.example.moxia.ui.theme.AppFilterChip
import top.yukonga.miuix.kmp.window.WindowDialog as MiuixWindowDialog

import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.example.moxia.R
import org.example.moxia.ui.theme.AppSecureTextField
import org.example.moxia.ui.theme.AppTextField
import org.example.moxia.ui.theme.navigationBarBottom
import org.example.moxia.ui.theme.statusBarTop
import org.example.moxia.api.MemosApiFactory
import org.example.moxia.api.SsoFailure
import org.example.moxia.api.SsoTransaction
import org.example.moxia.api.loginAndCreateToken
import org.example.moxia.api.normalizeLoginServerUrl
import org.example.moxia.model.Account
import org.example.moxia.model.IdentityProvider
import org.example.moxia.ui.setup.SsoLoginActivity

enum class LoginMode {
    PASSWORD, TOKEN, SSO
}

/** The label shown on the mode picker for each [LoginMode]. */
private val LoginMode.labelRes: Int
    get() = when (this) {
        LoginMode.PASSWORD -> R.string.login_password
        LoginMode.TOKEN -> R.string.login_token
        LoginMode.SSO -> R.string.login_sso
    }

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit, modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            LoginContent(
                onLoginSuccess = onLoginSuccess,
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    // The page is edge to edge: the form starts below the status bar and keeps
                    // clear of the gesture handle, while the page background fills both bands.
                    .padding(top = 64.dp + statusBarTop(), bottom = 24.dp + navigationBarBottom())
            )
            val backupContext = androidx.compose.ui.platform.LocalContext.current
            TextButton(onClick = { backupContext.startActivity(org.example.moxia.ui.backup.BackupTransferActivity.importIntent(backupContext)) }) {
                Text(stringResource(R.string.backup_restore_before_login))
            }
            }
        }
    }
}

/**
 * Login dialog that can also be used for editing existing account credentials.
 *
 * @param onLoginSuccess Callback with (baseUrl, token) on successful login/save
 * @param onDismiss Callback when dialog is dismissed
 * @param editAccount Optional - if provided, the dialog is in "edit mode" with pre-filled values
 */
@Composable
fun LoginDialog(
    onLoginSuccess: (String, String) -> Unit,
    onDismiss: () -> Unit,
    editAccount: Account? = null
) {
    // Miuix owns the window dialog here, so the form sits on its surface and corner radius.
    MiuixWindowDialog(show = true, onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.common_close))
            }
            LoginContent(
                onLoginSuccess = onLoginSuccess,
                modifier = Modifier.padding(top = 8.dp),
                editAccount = editAccount
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginContent(
    onLoginSuccess: (String, String) -> Unit,
    modifier: Modifier = Modifier,
    editAccount: Account? = null,
    showTitle: Boolean = true
) {
    // If editing, default to token mode and pre-fill values
    val isEditMode = editAccount != null
    var loginMode by rememberSaveable(editAccount?.id) { mutableStateOf(if (isEditMode) LoginMode.TOKEN else LoginMode.PASSWORD) }
    var hostUrl by rememberSaveable(editAccount?.id) { mutableStateOf(editAccount?.hostUrl ?: "") }
    var token by rememberSaveable(editAccount?.id) { mutableStateOf(editAccount?.accessToken ?: "") }
    var username by rememberSaveable(editAccount?.id) { mutableStateOf("") }
    val passwordState = rememberTextFieldState()
    val password = passwordState.text.toString()
    var isLoading by remember { mutableStateOf(false) }
    var ssoInProgress by rememberSaveable { mutableStateOf(false) }
    var providers by remember(hostUrl) { mutableStateOf<List<IdentityProvider>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val networkPermission = LocalNetworkPermission.current
    val localNetworkDenied = stringResource(R.string.local_network_permission_denied)

    val errorEmptyHost = stringResource(R.string.login_error_empty_host)
    val errorFailed = stringResource(R.string.login_error_failed)
    val errorInvalidUrl = stringResource(R.string.login_error_invalid_url)
    val errorInvalidInstance = stringResource(R.string.login_error_invalid_instance)
    val errorEmptyToken = stringResource(R.string.login_error_empty_token)
    val errorVerificationFailed = stringResource(R.string.login_error_verification_failed)
    val errorEmptyCredentials = stringResource(R.string.login_error_empty_credentials)
    val errorInvalidCredentials = stringResource(R.string.login_error_invalid_credentials)

    val context = LocalContext.current
    val ssoErrorMessages = mapOf(
        SsoFailure.INVALID_PROVIDER to stringResource(R.string.login_sso_invalid_provider),
        SsoFailure.INVALID_CALLBACK to stringResource(R.string.login_sso_invalid_callback),
        SsoFailure.EXPIRED to stringResource(R.string.login_sso_expired),
        SsoFailure.DENIED to stringResource(R.string.login_sso_denied),
        SsoFailure.BROWSER_UNAVAILABLE to stringResource(R.string.login_sso_no_browser),
        SsoFailure.SIGN_IN to stringResource(R.string.login_sso_failed),
        SsoFailure.TOKEN to stringResource(R.string.login_sso_token_failed)
    )
    val discoveryFailed = stringResource(R.string.login_sso_discovery_failed)
    val ssoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        ssoInProgress = false
        val credentials = SsoLoginActivity.readResult(result.resultCode, result.data)
        if (credentials.baseUrl != null && credentials.token != null) {
            onLoginSuccess(credentials.baseUrl, credentials.token)
        } else {
            errorMessage = credentials.error?.let { ssoErrorMessages[it] }
        }
    }
    val busy = isLoading || ssoInProgress

    val findProviders = {
        scope.launch {
            if (isLoading || ssoInProgress) return@launch
            errorMessage = null
            providers = null
            val baseUrl = normalizeLoginServerUrl(hostUrl)
            if (baseUrl == null) {
                errorMessage = if (hostUrl.isBlank()) errorEmptyHost else errorInvalidUrl
            } else {
                isLoading = true
                try {
                    if (!networkPermission.ensureAccess(baseUrl)) {
                        errorMessage = localNetworkDenied
                    } else {
                        val api = MemosApiFactory.create(baseUrl, OkHttpClient())
                        api.getInstanceProfile()
                        providers = api.listIdentityProviders().identityProviders.orEmpty().filter { provider ->
                            try {
                                SsoTransaction.create(baseUrl, provider, SsoLoginActivity.redirectUri(context))
                                true
                            } catch (_: Exception) { false }
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    errorMessage = discoveryFailed
                } finally {
                    isLoading = false
                }
            }
        }
    }

    val performLogin = {
        scope.launch {
            Log.d("MemosLogin", "performLogin: mode=$loginMode host=$hostUrl")
            if (isLoading || ssoInProgress) {
                Log.d("MemosLogin", "performLogin: already busy (isLoading=$isLoading sso=$ssoInProgress)")
                return@launch
            }
            isLoading = true
            errorMessage = null
            val baseUrl = normalizeLoginServerUrl(hostUrl)
            if (baseUrl == null) {
                errorMessage = if (hostUrl.isBlank()) errorEmptyHost else errorInvalidUrl
                isLoading = false
                return@launch
            }

            try {
                if (!networkPermission.ensureAccess(baseUrl)) {
                    errorMessage = localNetworkDenied
                    return@launch
                }
                val logging = HttpLoggingInterceptor { message ->
                    Log.d("MemosApi", message)
                }.apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }

                val client = OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build()

                val api = MemosApiFactory.create(baseUrl, client)

                // Check if instance is valid by fetching instance profile
                try {
                    api.getInstanceProfile()
                    Log.d("MemosLogin", "performLogin: instance profile ok")
                } catch (e: Exception) {
                    Log.e("MemosLogin", "performLogin: instance profile failed", e)
                    errorMessage = errorInvalidInstance
                    isLoading = false
                    return@launch
                }

                if (loginMode == LoginMode.TOKEN) {
                    val trimmedToken = token.trim()
                    if (trimmedToken.isBlank()) {
                        errorMessage = errorEmptyToken
                        isLoading = false
                        return@launch
                    }

                    // Verify token validity by fetching current user
                    val authClient =
                        OkHttpClient.Builder().addInterceptor(logging).addInterceptor { chain ->
                            val request = chain.request().newBuilder()
                                .addHeader("Authorization", "Bearer $trimmedToken").build()
                            chain.proceed(request)
                        }.build()

                    val authApi = MemosApiFactory.create(baseUrl, authClient)

                    try {
                        authApi.getCurrentSession()
                        Log.d("MemosLogin", "performLogin: token verified, handing over")
                        onLoginSuccess(baseUrl, trimmedToken)
                    } catch (e: Exception) {
                        Log.e("MemosLogin", "Token verification failed", e)
                        errorMessage = errorVerificationFailed
                    }
                } else {
                    if (username.isBlank() || password.isBlank()) {
                        errorMessage = errorEmptyCredentials
                        isLoading = false
                        return@launch
                    }

                    try {
                        val token = loginAndCreateToken(
                            api, baseUrl, username.trim(), password
                        )
                        onLoginSuccess(baseUrl, token)

                    } catch (e: Exception) {
                        Log.e("MemosLogin", "Login failed", e)
                        errorMessage = errorInvalidCredentials
                    }
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MemosLogin", "Login failed", e)
                errorMessage = errorFailed
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        if (showTitle) Text(
            text = if (isEditMode) stringResource(R.string.profile_edit_credentials) else stringResource(
                R.string.login_title
            ),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (Build.VERSION.SDK_INT >= 37 && !networkPermission.granted) {
            TextButton(onClick = { scope.launch { networkPermission.requestAccess() } }) {
                Text(stringResource(R.string.local_network_permission_title))
            }
        }

        // HyperOS expresses a small exclusive choice as separate pills instead of Material's
        // connected button group.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoginMode.entries.forEach { mode ->
                AppFilterChip(
                    selected = loginMode == mode,
                    onClick = {
                        loginMode = mode
                        errorMessage = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.RadioButton
                            selected = loginMode == mode
                        },
                    enabled = !busy
                ) {
                    Text(
                        text = stringResource(mode.labelRes),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppTextField(
            value = hostUrl,
            onValueChange = { hostUrl = it; errorMessage = null },
            label = stringResource(R.string.login_host_url),
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(R.string.login_host_url_placeholder),
            enabled = !busy,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (loginMode) {
            LoginMode.SSO -> {
                Text(stringResource(R.string.login_sso_description), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                providers?.let { available ->
                    if (available.isEmpty()) {
                        Text(stringResource(R.string.login_sso_no_providers))
                    }
                    available.forEach { provider ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    if (isLoading || ssoInProgress) return@launch
                                    errorMessage = null
                                    val baseUrl = normalizeLoginServerUrl(hostUrl)
                                    if (baseUrl == null) {
                                        errorMessage = errorInvalidUrl
                                        return@launch
                                    }
                                    isLoading = true
                                    try {
                                        if (!networkPermission.ensureAccess(baseUrl)) {
                                            errorMessage = localNetworkDenied
                                        } else {
                                            ssoInProgress = true
                                            ssoLauncher.launch(SsoLoginActivity.createIntent(context, baseUrl, provider))
                                        }
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (_: Exception) {
                                        ssoInProgress = false
                                        errorMessage = ssoErrorMessages[SsoFailure.SIGN_IN]
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.login_sso_continue_with, provider.title))
                        }
                    }
                }
            }
            LoginMode.TOKEN -> {
                AppTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = stringResource(R.string.login_token),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { performLogin() })
                )
            }

            LoginMode.PASSWORD -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AppTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = stringResource(R.string.login_username),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppSecureTextField(
                        state = passwordState,
                        label = stringResource(R.string.login_password),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        onDone = { performLogin() }
                    )
                }
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if (loginMode == LoginMode.SSO) findProviders() else performLogin() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy && hostUrl.isNotBlank()
        ) {
            if (busy) {
                AppCircularProgressIndicator(size = 24.dp, strokeWidth = 2.dp)
            } else {
                Text(when {
                    loginMode == LoginMode.SSO -> stringResource(R.string.login_sso_find_providers)
                    isEditMode -> stringResource(R.string.common_save)
                    else -> stringResource(R.string.login_button)
                })
            }
        }
    }
}
