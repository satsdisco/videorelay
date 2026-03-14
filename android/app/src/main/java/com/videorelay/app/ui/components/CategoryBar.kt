package com.videorelay.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videorelay.app.data.nostr.NostrConstants

@Composable
fun CategoryBar(
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // "All" chip
        FilterChip(
            selected = selectedTag == null,
            onClick = { onTagSelected(null) },
            label = { Text("All") },
        )

        NostrConstants.CATEGORY_TAGS.forEach { tag ->
            FilterChip(
                selected = selectedTag == tag,
                onClick = { onTagSelected(if (selectedTag == tag) null else tag) },
                label = { Text(tag.replaceFirstChar { it.uppercase() }) },
            )
        }
    }
}
