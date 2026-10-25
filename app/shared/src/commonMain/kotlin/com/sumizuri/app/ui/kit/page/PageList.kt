package com.sumizuri.app.ui.kit.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sumizuri.app.ui.kit.scroll.RegisterScrollTarget
import com.sumizuri.app.ui.kit.scroll.mouseDragScroll

// The long list every page uses, with the page gutter and the same gap between rows.
@Composable
fun PageList(modifier: Modifier = Modifier, gap: Dp = 8.dp, content: LazyListScope.() -> Unit) {
    val state = rememberLazyListState()
    RegisterScrollTarget(state)
    LazyColumn(
        modifier.mouseDragScroll(state),
        state = state,
        contentPadding = PaddingValues(
            start = PageGutter,
            end = PageGutter,
            top = 8.dp,
            bottom =
            8.dp + LocalBottomClearance.current,
        ),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}
