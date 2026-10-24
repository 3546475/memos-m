package org.example.moxia.ui.component.item

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.example.moxia.R
import org.example.moxia.ui.theme.AppDialog
import org.example.moxia.ui.theme.AppTextButton

/**
 * Shows the finished image before it leaves the app: sharing a memo as a picture is irreversible
 * once it is pasted somewhere, so the user sees exactly what will be sent — and can keep a copy
 * instead.
 */
@Composable
internal fun MemoSharePreviewDialog(
    image: ImageBitmap,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AppDialog(
        show = true,
        title = stringResource(R.string.memo_share_image_title),
        onDismissRequest = onDismiss,
        buttons = {
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            )
            AppTextButton(
                text = stringResource(R.string.memo_share_image_save),
                modifier = Modifier.weight(1f),
                onClick = onSave
            )
            AppTextButton(
                text = stringResource(R.string.memo_action_share_image),
                modifier = Modifier.weight(1f),
                onClick = onShare
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
