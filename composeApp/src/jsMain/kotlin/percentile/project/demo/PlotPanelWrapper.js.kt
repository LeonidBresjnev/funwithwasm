package percentile.project.demo

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.letsPlot.Figure

@Composable
actual fun PlotPanel(figure: Figure, modifier: Modifier) {
    Text("PlotPanel not supported on JS platform", modifier = modifier)
}
