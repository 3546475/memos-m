package org.example.memosm.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.example.memosm.data.cache.CacheListType
import org.example.memosm.data.cache.MemoCacheRepository
import org.example.memosm.model.Memo
import org.example.memosm.viewmodel.MemosViewModel
import org.koin.compose.koinInject

/**
 * Everything this account can be filtered on locally: the whole user feed (paged in, because the
 * feed otherwise only holds the first page) plus the comments cached from detail pages. Both the
 * profile's counts and the per-kind lists read this, so a number and the list behind it agree
 * instead of coming from two different data sets.
 */
@Composable
fun rememberLocalMemoCatalog(viewModel: MemosViewModel): List<Memo> {
    val uiState by viewModel.uiState.collectAsState()
    val accountId = uiState.accounts.firstOrNull { it.isActive }?.id
    val cache: MemoCacheRepository = koinInject()
    var comments by remember(accountId) { mutableStateOf(emptyList<Memo>()) }

    LaunchedEffect(accountId) {
        viewModel.loadAllUserMemos()
        if (accountId != null) {
            comments = cache.getCachedMemos(accountId, CacheListType.COMMENT)
        }
    }

    return remember(uiState.userMemoList.list.items, comments) {
        (uiState.userMemoList.list.items + comments)
            .distinctBy { it.name ?: it.content.hashCode().toString() }
    }
}
