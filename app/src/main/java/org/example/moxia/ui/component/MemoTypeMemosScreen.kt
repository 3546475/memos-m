package org.example.moxia.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.example.moxia.R
import org.example.moxia.model.MemoContentType
import org.example.moxia.model.matchesContentType
import org.example.moxia.ui.theme.AppTopBar
import org.example.moxia.viewmodel.MemosViewModel

/**
 * The memos of one [MemoContentType]. There is no server-side filter for the type, so the list is
 * picked out of what this device has loaded — the feed plus the comments cached from the detail
 * pages, because the server counts both. Pull-to-refresh fetches another page of the feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoTypeMemosScreen(
    viewModel: MemosViewModel,
    type: MemoContentType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    // The type has no server-side filter, so the list is built from the same local catalog the
    // profile counts come from: every page of the feed plus the cached comments.
    val catalog = rememberLocalMemoCatalog(viewModel)
    val memos = remember(catalog, type) {
        catalog.filter { it.matchesContentType(type) }
    }
    val title = stringResource(
        when (type) {
            MemoContentType.LINK -> R.string.profile_stats_links
            MemoContentType.CODE -> R.string.profile_stats_code
            MemoContentType.TODO -> R.string.profile_stats_todo
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        MemosScaffold(
            viewModel = viewModel,
            memos = memos,
            listState = listState,
            isNavBarVisible = false,
            topBar = { isDetailVisible, isDualPane ->
                if (!isDetailVisible || isDualPane) {
                    AppTopBar(title = title, navigationIcon = { ProfileBackButton(onClick = onBack) })
                }
            },
            listPane = { onMemoClick ->
                GenericMemosListPane(
                    viewModel = viewModel,
                    memos = memos,
                    isLoading = uiState.userMemoList.list.isLoading,
                    isRefreshing = uiState.isRefreshing,
                    nextPageToken = uiState.userMemoList.list.nextPageToken,
                    onLoadMore = { viewModel.loadMoreUserMemos() },
                    onRefresh = { viewModel.fetchUserMemos(refresh = true) },
                    onMemoClick = onMemoClick,
                    listState = listState,
                    userProvider = { uiState.session.currUser },
                    showSkeleton = !uiState.userMemoList.list.initialLoadFinished,
                    contentPadding = PaddingValues(16.dp)
                )
            })
    }
}
