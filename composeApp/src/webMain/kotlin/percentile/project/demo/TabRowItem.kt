package percentile.project.demo

import androidx.compose.runtime.Composable

data class TabRowItem(
    val title: String,
    val screen: @Composable () -> Unit,
    val enabled: Boolean = true
)