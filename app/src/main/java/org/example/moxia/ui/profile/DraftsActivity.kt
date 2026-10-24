package org.example.moxia.ui.profile

import androidx.compose.runtime.Composable
import org.example.moxia.ui.nav.DraftsScreen
import org.example.moxia.viewmodel.MemosViewModel

class DraftsActivity : ProfileActivity() {
    @Composable
    override fun Destination(viewModel: MemosViewModel, onBack: () -> Unit) {
        DraftsScreen(viewModel = viewModel, onBack = onBack)
    }
}
