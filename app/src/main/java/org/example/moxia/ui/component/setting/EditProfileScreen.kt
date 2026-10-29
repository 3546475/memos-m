package org.example.moxia.ui.component.setting

import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import androidx.core.net.toUri
import com.mr0xf00.easycrop.CropResult
import com.mr0xf00.easycrop.CropperStyle
import com.mr0xf00.easycrop.crop
import com.mr0xf00.easycrop.rememberImageCropper
import com.mr0xf00.easycrop.ui.ImageCropperDialog
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.moxia.ui.theme.AppCircularProgressIndicator
import org.example.moxia.ui.theme.AppTextField
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.example.moxia.ui.theme.AppSecureTextField
import org.example.moxia.ui.theme.AppTopBar
import org.example.moxia.ui.theme.navigationBarBottom
import org.example.moxia.ui.theme.pageContentWindowInsets
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.example.moxia.R
import org.example.moxia.model.Account
import org.example.moxia.ui.component.ProfileBackButton
import org.example.moxia.ui.component.resolveResourceUrl

data class UserProfileUpdate(
    val username: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val description: String? = null,
    val password: String? = null
)

/** Longest edge of the stored avatar; anything larger is wasted on an 88dp circle. */
private const val AvatarMaxSize = 256

/** JPEG quality for the stored avatar: visually lossless at this size, a fraction of the bytes. */
private const val AvatarJpegQuality = 85

/**
 * Encodes the cropped picture as the inline data URI the server accepts for `avatarUrl`
 * (`invalid avatar format: invalid data URI format` is what a plain path returns). The image is
 * scaled to [AvatarMaxSize] and JPEG-encoded first: a data URI lives in the database row, so a
 * multi-megabyte base64 string would be stored and shipped with every user payload.
 */
private fun Bitmap.toAvatarDataUri(): String? = runCatching {
    val scale = AvatarMaxSize.toFloat() / maxOf(width, height).coerceAtLeast(1)
    val scaled = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            this,
            (width * scale).toInt().coerceAtLeast(1),
            (height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        this
    }
    val bytes = ByteArrayOutputStream().use { stream ->
        scaled.compress(Bitmap.CompressFormat.JPEG, AvatarJpegQuality, stream)
        stream.toByteArray()
    }
    "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    account: Account,
    onBack: () -> Unit,
    onSave: (UserProfileUpdate) -> Unit,
    isSaving: Boolean = false,
    canSave: Boolean = true,
    errorMessage: String? = null
) {
    var username by rememberSaveable(account.id) { mutableStateOf(account.name ?: "") }
    var email by rememberSaveable(account.id) { mutableStateOf(account.email ?: "") }
    var displayName by rememberSaveable(account.id) { mutableStateOf(account.displayName ?: "") }
    var avatarUrl by rememberSaveable(account.id) { mutableStateOf(account.avatarUrl ?: "") }
    var description by rememberSaveable(account.id) { mutableStateOf(account.description ?: "") }
    val passwordState = rememberTextFieldState()
    val password = passwordState.text.toString()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var avatarBusy by remember { mutableStateOf(false) }
    var avatarError by remember { mutableStateOf<String?>(null) }
    val avatarFailed = stringResource(R.string.profile_avatar_upload_failed)
    // The picker only chooses a file; the cropper decides the framing.
    val imageCropper = rememberImageCropper()
    val cropState = imageCropper.cropState
    // Miuix dark surface: a black canvas with a soft scrim outside the crop box and no Material
    // guidelines grid.
    val avatarCropStyle = CropperStyle(
        backgroundColor = Color.Black,
        overlay = Color.Black.copy(alpha = 0.6f),
        autoZoom = true,
        guidelines = null
    )
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                avatarError = null
                val cropped = imageCropper.crop(uri, context)
                if (cropped !is CropResult.Success) return@launch
                avatarBusy = true
                // The server stores avatars as an inline data URI, so the cropped picture is
                // scaled down and base64-encoded here instead of being uploaded as a file.
                val dataUri = withContext(Dispatchers.IO) {
                    cropped.bitmap.asAndroidBitmap().toAvatarDataUri()
                }
                avatarBusy = false
                if (dataUri == null) avatarError = avatarFailed else avatarUrl = dataUri
            }
        }
    }
    val avatarEnabled = !isSaving && !avatarBusy

    if (cropState != null) {
        // Start on a centred square: an avatar is always shown in a circle, so a free-form first
        // frame makes the user do the framing every time. The toolbar still changes the ratio.
        LaunchedEffect(cropState) {
            withFrameNanos { }
            val size = cropState.src.size
            if (size.width > 0 && size.height > 0) {
                val side = minOf(size.width, size.height).toFloat()
                cropState.region = Rect(
                    offset = Offset((size.width - side) / 2f, (size.height - side) / 2f),
                    size = Size(side, side)
                )
                cropState.aspectLock = true
            }
        }
        ImageCropperDialog(
            state = cropState,
            style = avatarCropStyle,
            topBar = { state ->
                // The dialog surface under this bar is the app's light surface, so the bar paints
                // its own dark background; otherwise white labels sat on white and vanished.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { state.done(false) }) {
                        Text(
                            text = stringResource(R.string.common_cancel),
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.profile_avatar_crop_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { state.done(true) }) {
                        Text(
                            text = stringResource(R.string.common_done),
                            color = Color.White
                        )
                    }
                }
            }
        )
    }

    // Idle Back is owned by Android so it can preview the profile activity underneath.
    BackHandler(enabled = isSaving) {}

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        // The form scrolls under the gesture handle; the column below keeps that inset as its
        // own bottom padding.
        contentWindowInsets = pageContentWindowInsets(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.profile_edit_account),
                navigationIcon = {
                    ProfileBackButton(onClick = { if (!isSaving) onBack() })
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSave(
                                UserProfileUpdate(
                                    username = username.takeIf { it != (account.name ?: "") },
                                    email = email.takeIf { it != (account.email ?: "") },
                                    displayName = displayName.takeIf { it != (account.displayName ?: "") },
                                    avatarUrl = avatarUrl.takeIf { it != (account.avatarUrl ?: "") },
                                    description = description.takeIf { it != (account.description ?: "") },
                                    password = password.takeIf { it.isNotBlank() }
                                )
                            )
                        },
                        enabled = !isSaving && canSave
                    ) {
                        if (isSaving) {
                            AppCircularProgressIndicator(
                                size = 16.dp,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.common_save))
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navigationBarBottom()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                }

                // Avatar preview; tapping it uploads a new picture and fills the URL field below.
                Box(contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = resolveResourceUrl(account.hostUrl, avatarUrl.ifBlank { account.avatarUrl }),
                        contentDescription = null,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .clickable(enabled = avatarEnabled) { pickAvatar.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                    if (avatarBusy) {
                        AppCircularProgressIndicator(
                            size = 28.dp,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(4.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                avatarError?.let { message ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                AppTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = stringResource(R.string.profile_username),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                    )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = stringResource(R.string.profile_display_name),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                    )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(R.string.profile_email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                    )

                Spacer(modifier = Modifier.height(12.dp))

                if (avatarUrl.startsWith("data:")) {
                    // The generated value is a base64 data URI, so it is not shown in a text field.
                    // This is still the same field component with readOnly, which keeps the
                    // background, height, padding and corner radius identical to the URL field it
                    // replaces; a settings row looked like a different control in this form.
                    AppTextField(
                        value = stringResource(R.string.profile_avatar_generated),
                        onValueChange = {},
                        label = stringResource(R.string.profile_avatar_url),
                        singleLine = true,
                        readOnly = true,
                        trailingIcon = {
                            TextButton(onClick = { avatarUrl = "" }, enabled = !isSaving) {
                                Text(stringResource(R.string.profile_avatar_use_url))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    AppTextField(
                        value = avatarUrl,
                        onValueChange = { avatarUrl = it },
                        label = stringResource(R.string.profile_avatar_url),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.profile_description),
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppSecureTextField(
                    state = passwordState,
                    label = stringResource(R.string.profile_new_password),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    supportingText = stringResource(R.string.profile_password_hint)
                )
            }
        }
    }
}
