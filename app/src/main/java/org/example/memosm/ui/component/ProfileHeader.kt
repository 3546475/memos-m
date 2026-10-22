package org.example.memosm.ui.component

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import org.example.memosm.R
import org.example.memosm.model.User
import org.example.memosm.model.UserStats
import org.example.memosm.ui.component.item.media.MemoImage
import org.example.memosm.ui.theme.AppCard
import org.example.memosm.ui.theme.AppDivider

/**
 * The identity block at the top of the profile page. HyperOS draws this on the page background
 * rather than inside a card, keeps it to two lines of text, and leaves the actions to the grouped
 * list below — a card here made the header compete with the sections it introduces.
 */
@Composable
fun ProfileHeader(
    user: User?,
    onClick: () -> Unit
) {
    val unknown = stringResource(R.string.memo_unknown_user)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MemoImage(
            uri = user?.avatarUrl?.toUri() ?: Uri.EMPTY,
            token = user?.token,
            modifier = Modifier.size(72.dp),
            isRound = true,
            placeholderIcon = Icons.Outlined.AccountCircle
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = user?.displayName ?: unknown,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = user?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: unknown,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val description = user?.description
        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

/**
 * Summary counts as one flat row of icon + number, directly under the identity block and on the
 * page background: they describe the profile, they are not a card competing with it, and opening a
 * list is the list's job, not the tally's.
 */
@Composable
fun ProfileStatsRow(
    userStats: UserStats?,
    modifier: Modifier = Modifier,
    /** Local counts, so a number matches the list behind it. */
    linkCount: Int? = null,
    codeCount: Int? = null,
    todoCount: Int? = null
) {
    val notAvailable = stringResource(R.string.common_not_available)
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ProfileStatItem(
            label = stringResource(R.string.profile_stats_memos),
            value = userStats?.totalMemoCount?.toString() ?: notAvailable,
            icon = Icons.AutoMirrored.Outlined.LibraryBooks,
            modifier = Modifier.weight(1f)
        )
        ProfileStatItem(
            label = stringResource(R.string.profile_stats_tags),
            value = userStats?.tagCount?.size?.toString() ?: notAvailable,
            icon = Icons.Outlined.Tag,
            modifier = Modifier.weight(1f)
        )
        ProfileStatItem(
            label = stringResource(R.string.profile_stats_pinned),
            value = userStats?.pinnedMemos?.size?.toString() ?: notAvailable,
            icon = Icons.Outlined.PushPin,
            modifier = Modifier.weight(1f)
        )
        ProfileStatItem(
            label = stringResource(R.string.profile_stats_links),
            value = (linkCount ?: userStats?.memoTypeStats?.linkCount)?.toString() ?: notAvailable,
            icon = Icons.Outlined.Link,
            modifier = Modifier.weight(1f)
        )
        ProfileStatItem(
            label = stringResource(R.string.profile_stats_code),
            value = (codeCount ?: userStats?.memoTypeStats?.codeCount)?.toString() ?: notAvailable,
            icon = Icons.Outlined.Code,
            modifier = Modifier.weight(1f)
        )
        ProfileStatItem(
            label = stringResource(R.string.profile_stats_todo),
            value = (todoCount ?: userStats?.memoTypeStats?.todoCount)?.toString() ?: notAvailable,
            icon = Icons.Outlined.TaskAlt,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = "$label: $value" }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false
        )
    }
}