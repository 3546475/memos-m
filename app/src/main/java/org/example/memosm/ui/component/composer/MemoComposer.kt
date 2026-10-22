package org.example.memosm.ui.component.composer

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.memosm.R
import org.example.memosm.ui.theme.AppDialog
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import org.example.memosm.ui.theme.bottomBarBlurSupported
import org.example.memosm.ui.theme.AppTextButton
import org.example.memosm.ui.theme.AppTextField
import org.example.memosm.data.base64ToTempUri
import org.example.memosm.data.uriToBase64Attachment
import org.example.memosm.model.Attachment
import org.example.memosm.model.Location
import org.example.memosm.model.Memo
import org.example.memosm.ui.component.item.markdown.plainLabel
import org.example.memosm.model.Visibility
import org.example.memosm.ui.findActivity
import org.example.memosm.ui.getFileSize


/**
 * Mode for the memo composer, determines the submit button label.
 * All callers MUST specify an explicit mode.
 */
enum class ComposerMode {
    /** Creating a new memo or publishing a draft */
    PUBLISH,

    /** Editing an existing memo */
    UPDATE,

    /** Adding a comment to a memo */
    COMMENT
}


@Composable
fun MemoComposer(
    modifier: Modifier = Modifier,
    onPublish: (String, Visibility, List<Attachment>, Location?) -> Unit,
    onUploadFile: suspend (Uri, Context) -> Attachment?,
    availableTags: Map<String, Int>,
    token: String,
    hostUrl: String,
    mode: ComposerMode,
    isPosting: Boolean = false,
    /** Request the keyboard on open. Off by default: entering the editor is not a typing gesture. */
    autoFocus: Boolean = false,
    editorState: ComposerState,
    /** The user's own memos, for the "link memo" picker. */
    linkableMemos: List<Memo> = emptyList(),
    onDiscardQueuedUpload: (String) -> Unit = {}

) {
    // Depending on the Composer Mode there will be different placeholder
    val placeholder = when (mode) {
        ComposerMode.PUBLISH -> stringResource(R.string.memo_composer_placeholder)
        ComposerMode.UPDATE -> stringResource(R.string.memo_composer_placeholder)
        ComposerMode.COMMENT -> stringResource(R.string.memo_detail_comment_placeholder)
    }
    val context = LocalContext.current
    val fileTooLargeMessage = stringResource(R.string.memo_composer_error_file_too_large)

    var contentState by editorState.content
    var visibility by editorState.visibility
    var location by editorState.location
    var draftAttachments by editorState.attachments

    var uploadingUris by remember { mutableStateOf(setOf<Uri>()) }
    var isUploadingCount by remember { mutableIntStateOf(0) }
    var showLocationEditDialog by remember { mutableStateOf(false) }
    var linkedMemoNames by editorState.linkedMemoNames
    var showLinkPicker by remember { mutableStateOf(false) }
    // Height of the action bar, so the link chips can float just above it.
    var barHeightPx by remember { mutableIntStateOf(0) }
    // The chip needs something readable: the memo's first line, falling back to its server name.
    val linkedMemos = linkedMemoNames.map { name ->
        name to (linkableMemos.firstOrNull { it.name == name }?.plainLabel() ?: name)
    }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Convert initialUris (from share intent) to base64 attachments in background
    // This mirrors the picker flow so they persist when the draft is saved
    LaunchedEffect(Unit) {
        draftAttachments.filter { it.second == null }.map { it.first }.forEach { uri ->
            scope.launch {
                val attachment = uriToBase64Attachment(uri, context)
                if (attachment != null) {
                    draftAttachments = draftAttachments.map { (u, a) ->
                        if (u == uri && a == null) uri to attachment else u to a
                    }
                }
            }
        }
    }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Add URIs immediately for display, then convert to base64 in background
            val newUris = uris.map { it to null as Attachment? }
            draftAttachments = draftAttachments + newUris

            // Convert to base64 in background for each URI
            uris.forEach { uri ->
                scope.launch {
                    val attachment = uriToBase64Attachment(uri, context)
                    if (attachment != null) {
                        // Update the draft attachments with the converted attachment
                        draftAttachments = draftAttachments.map { (u, a) ->
                            if (u == uri && a == null) uri to attachment else u to a
                        }
                    }
                }
            }
        }
    }

    // Drag and Drop state
    var isDragging by remember { mutableStateOf(false) }
    val dndTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragging = false
                val dragEvent = event.toAndroidDragEvent()

                // Request drag and drop permissions for cross-app access
                context.findActivity()?.requestDragAndDropPermissions(dragEvent)

                val clipData = dragEvent.clipData
                if (clipData != null) {
                    val uris = mutableListOf<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        clipData.getItemAt(i).uri?.let { uris.add(it) }
                    }
                    if (uris.isNotEmpty()) {
                        scope.launch {
                            val validUris = uris.filter { uri ->
                                val size = getFileSize(context, uri)
                                if (size > 10 * 1024 * 1024) {
                                    Toast.makeText(
                                        context, fileTooLargeMessage, Toast.LENGTH_SHORT
                                    ).show()
                                    false
                                } else {
                                    true
                                }
                            }

                            val newAttachments = validUris.map { uri ->
                                val base64Attachment = uriToBase64Attachment(uri, context)
                                uri to base64Attachment
                            }
                            draftAttachments = draftAttachments + newAttachments
                        }
                        return true
                    }
                }
                return false
            }

            override fun onEntered(event: DragAndDropEvent) {
                isDragging = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDragging = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragging = false
            }
        }
    }

    var componentWidth by remember { mutableStateOf(0.dp) }

    // The editor content runs to the bottom of the screen and the action bar floats over it, so the
    // bar sits on the text instead of on a column and can frost it.
    val composerBackdrop = rememberLayerBackdrop()
    val blurAvailable = bottomBarBlurSupported()
    // The bar clears the gesture bar, and the keyboard when it is up. One value derived from the
    // insets themselves — not a branch on "is the keyboard visible" — so the bar moves with the
    // animation instead of dropping past its resting place and snapping back.
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val barBottomInset = maxOf(navBarBottom, imeBottom)

    Box(
        modifier = modifier
            .onSizeChanged {
                componentWidth = with(density) { it.width.toDp() }
            }
            .dragAndDropTarget(
                shouldStartDragAndDrop = { true }, target = dndTarget
            )
            .background(
                if (isDragging) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else Color.Transparent, RoundedCornerShape(8.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurAvailable) Modifier.layerBackdrop(composerBackdrop) else Modifier
                )
                // Painted *after* layerBackdrop, so the page colour is part of the recorded layer.
                // Without it the layer holds only the glyphs and is transparent everywhere else;
                // blurring those pixels means averaging black against transparent, which the blur
                // premultiplies into a near-black backdrop — the grey block. The feed's recorded
                // layer is opaque because its pane paints a surface inside it.
                .background(MaterialTheme.colorScheme.background)
        ) {
            MemoInput(
                contentState = contentState,
                onContentChange = { contentState = it },
                placeholder = placeholder,
                availableTags = availableTags,
                enabled = !isPosting,
                // Opening the editor does not pop the keyboard: the user decides when to type.
                autoFocus = autoFocus,
                // Full bleed: the bar floats over the text, and the scroll room it needs after the
                // last line comes from the trailing blank lines the markdown transformation adds,
                // not from an inset here (which would stop the text at the bar's edge).
                // The text keeps clear of the keyboard; the bar floats over the page instead of
                // being pushed up by a padding on the whole screen.
                modifier = Modifier.fillMaxSize().imePadding(),
            )
        }

        // Link chips live outside the action bar: they show state, the bar holds actions. Floating
        // them above the bar's measured height keeps them off both the text and the bar's surface.
        if (linkedMemos.isNotEmpty() && barHeightPx > 0) {
            LinkedMemoChips(
                linkedMemos = linkedMemos,
                onRemove = { name -> linkedMemoNames = linkedMemoNames - name },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(
                        // barHeightPx already includes the bar's own bottom inset.
                        bottom = with(density) { barHeightPx.toDp() } + 4.dp
                    )
            )
        }

        MemoComposerBottomBar(
            backdrop = if (blurAvailable) composerBackdrop else null,
            modifier = Modifier
                .onSizeChanged { barHeightPx = it.height }
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = barBottomInset),
            draftAttachments = draftAttachments,
            uploadingUris = uploadingUris,
            location = location,
            isPosting = isPosting,
            visibility = visibility,
            mode = mode,
            componentWidth = componentWidth,
            pickerLauncher = pickerLauncher,
            token = token,
            hostUrl = hostUrl,
            contentStateText = contentState.text,
            isUploadingCount = isUploadingCount,
            onRemoveLocation = { location = null },
            onLocationClick = { showLocationEditDialog = true },
            onLinkMemoClick = { showLinkPicker = true },
            onVisibilityChange = { visibility = it },
            onRemoveAttachment = { index ->
                if (index in draftAttachments.indices) {
                    val removed = draftAttachments[index].second
                    val updated = draftAttachments.toMutableList().apply { removeAt(index) }
                    draftAttachments = updated
                    // A removed placeholder (no server name yet) may leave its
                    // queued upload orphaned: nothing would ever reference the
                    // attachment once it lands. Discard it unless a draft or a
                    // queued outbox op still carries the same clientId - an
                    // orphan on the server beats losing bytes still referenced.
                    val clientId = removed?.clientId
                    if (removed != null && removed.name == null && clientId != null) {
                        scope.launch {
                            // Fire after the debounced draft save (500ms above)
                            // has persisted the attachment list without this
                            // placeholder; checking earlier would always find
                            // the stale draft still referencing the clientId.
                            delay(600)
                            onDiscardQueuedUpload(clientId)
                        }
                    }
                }
            },
            onRecordingFinished = { uri, attachment ->
                // Add immediately for display if attachment is null (just Uri)
                // Or update if attachment is ready
                draftAttachments = if (attachment == null) {
                    draftAttachments + (uri to null)
                } else {
                    draftAttachments.map { (u, a) ->
                        if (u == uri && a == null) uri to attachment else u to a
                    }
                }
            },
            onLocationFounded = { loc ->
                location = loc
            },
            onPublishClick = publish@{
                if (isPosting || isUploadingCount > 0) return@publish
                scope.launch {
                    val pendingUploads = draftAttachments.withIndex().filter { indexedValue ->
                        val pair = indexedValue.value
                        // Case 1: New local file (Uri is not EMPTY, Attachment is null)
                        (pair.second == null && pair.first != Uri.EMPTY) ||
                                // Case 2: Restored draft (Uri is EMPTY, Attachment has content but no name on server)
                                (pair.second != null && pair.second!!.name == null && pair.second!!.content != null)
                    }
                    val uploadedAttachments = mutableListOf<Attachment>()

                    for (indexedValue in pendingUploads) {
                        val index = indexedValue.index
                        val (uri, attachment) = indexedValue.value
                        isUploadingCount++

                        // If it's a restored draft, we first need to convert it to a temp Uri
                        val uploadUri = if (uri == Uri.EMPTY && attachment != null) {
                            base64ToTempUri(
                                attachment.content ?: "",
                                attachment.filename,
                                attachment.type,
                                context
                            )
                        } else {
                            uri
                        }

                        if (uploadUri != null) {
                            uploadingUris = uploadingUris + uploadUri
                            val uploaded = onUploadFile(uploadUri, context)
                            uploadingUris = uploadingUris - uploadUri

                            if (uploaded != null) {
                                uploadedAttachments.add(uploaded)
                                // Update local draft state immediately as we upload
                                // We match by the index to replace it safely
                                draftAttachments = draftAttachments.mapIndexed { i, item ->
                                    if (i == index) uploadUri to uploaded else item
                                }
                            }
                        }
                        isUploadingCount--
                    }

                    // Now collect all valid attachments:
                    // 1. Items that were already valid (non-null attachment with name)
                    // 2. Newly uploaded items are already swapped into draftAttachments by the loop above,
                    //    OR added to uploadedAttachments if we want to be safe, but the loop updates draftAttachments.
                    // 3. Queued-upload placeholders (name == null but carrying the queue
                    //    clientId) stay attached; the outbox replay resolves them to the
                    //    real server attachment once the durable upload lands.
                    val finalAttachments =
                        draftAttachments.mapNotNull { it.second }
                            .filter { it.name != null || it.clientId != null }

                    onPublish(
                        contentState.text, visibility, finalAttachments, location
                    )
                }
            })
    }

    if (showLinkPicker) {
        MemoLinkPickerDialog(
            memos = linkableMemos,
            excludedNames = linkedMemoNames.toSet(),
            onDismiss = { showLinkPicker = false },
            onSelect = { memo ->
                val name = memo.name
                if (name != null && name !in linkedMemoNames) {
                    linkedMemoNames = linkedMemoNames + name
                }
                showLinkPicker = false
            }
        )
    }

    if (showLocationEditDialog && location != null) {
        var tempPlaceholder by remember { mutableStateOf(location?.placeholder ?: "") }
        var tempLatitude by remember { mutableStateOf(location?.latitude?.toString() ?: "") }
        var tempLongitude by remember { mutableStateOf(location?.longitude?.toString() ?: "") }

        AppDialog(
            show = true,
            title = stringResource(R.string.memo_composer_edit_location),
            onDismissRequest = { showLocationEditDialog = false },
            buttons = {
                AppTextButton(
                    text = stringResource(R.string.common_cancel),
                    modifier = Modifier.weight(1f),
                    onClick = { showLocationEditDialog = false }
                )
                AppTextButton(
                    text = stringResource(R.string.common_save),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        location = location?.copy(
                            placeholder = tempPlaceholder,
                            latitude = tempLatitude.toDoubleOrNull(),
                            longitude = tempLongitude.toDoubleOrNull()
                        )
                        showLocationEditDialog = false
                    }
                )
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    value = tempPlaceholder,
                    onValueChange = { tempPlaceholder = it },
                    label = stringResource(R.string.memo_composer_location_name),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = tempLatitude,
                    onValueChange = { tempLatitude = it },
                    label = stringResource(R.string.memo_composer_location_latitude),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                AppTextField(
                    value = tempLongitude,
                    onValueChange = { tempLongitude = it },
                    label = stringResource(R.string.memo_composer_location_longitude),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Chips for the memos this one links to; four characters is all a chip is worth. */
@Composable
private fun LinkedMemoChips(
    linkedMemos: List<Pair<String, String>>,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(linkedMemos, key = { it.first }) { (name, label) ->
            InputChip(
                selected = true,
                onClick = {},
                shape = CircleShape,
                label = {
                    Text(
                        text = if (label.length > ChipLabelLength) {
                            label.take(ChipLabelLength) + "…"
                        } else {
                            label
                        },
                        maxLines = 1
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.memo_composer_remove_link),
                        modifier = Modifier
                            .size(16.dp)
                            .noRippleClickable { onRemove(name) }
                    )
                }
            )
        }
    }
}

private const val ChipLabelLength = 4