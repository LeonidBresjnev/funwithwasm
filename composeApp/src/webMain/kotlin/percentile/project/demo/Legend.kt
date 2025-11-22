package percentile.project.demo

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import io.github.koalaplot.core.Symbol
import io.github.koalaplot.core.legend.FlowLegend
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun Legend(legends: List<String>, colors: List<Color>) {
    val colorMap = buildMap {
        legends.forEachIndexed { idx, it ->
            put(it, colors[idx])
        }
    }

    FlowLegend(
        itemCount = legends.size,
        symbol = { i ->
            Symbol(
                fillBrush = SolidColor(colorMap[legends[i]] ?: Color.Black)
            )
        },
        label = { i ->
            Text(legends[i])
        }
    )


}