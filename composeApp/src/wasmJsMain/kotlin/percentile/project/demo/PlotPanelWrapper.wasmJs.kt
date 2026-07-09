package percentile.project.demo

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.letsPlot.Figure
import org.jetbrains.letsPlot.compose.PlotPanel

@Composable
actual fun PlotPanel(figure: Figure, modifier: Modifier) {
    PlotPanel(
        figure = figure,
        modifier = modifier
    ) { computationMessages ->
        computationMessages.forEach { println("[DEMO APP MESSAGE] $it") }
    }
}
