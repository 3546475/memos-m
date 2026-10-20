import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import org.example.memosm.R
import org.example.memosm.model.Memo
import org.example.memosm.ui.theme.AppDialog
import org.example.memosm.ui.theme.AppTextButton

@Composable
fun DeleteConfirmationDialog(
    memo: Memo, onDismiss: () -> Unit, onConfirm: () -> Unit
) {
    AppDialog(
        show = true,
        title = stringResource(R.string.memo_dialog_delete_title),
        summary = stringResource(R.string.memo_dialog_delete_confirm),
        onDismissRequest = onDismiss,
        buttons = {
            AppTextButton(
                text = stringResource(R.string.common_cancel),
                modifier = Modifier.weight(1f),
                onClick = onDismiss
            )
            AppTextButton(
                text = stringResource(R.string.memo_action_delete),
                modifier = Modifier.weight(1f),
                destructive = true,
                onClick = onConfirm
            )
        }
    )
}
