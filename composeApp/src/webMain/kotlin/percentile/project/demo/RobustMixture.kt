package percentile.project.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.ChartLayout
import io.github.koalaplot.core.legend.LegendLocation
import io.github.koalaplot.core.line.LinePlot
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.util.generateHueColorPalette
import io.github.koalaplot.core.xygraph.DefaultPoint
import io.github.koalaplot.core.xygraph.FloatLinearAxisModel
import io.github.koalaplot.core.xygraph.XYGraph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class, ExperimentalKoalaPlotApi::class)
@Composable
fun RobustMixture() {
    var ns by remember { mutableIntStateOf(100) }
    var ps by remember { mutableFloatStateOf(0.5f) }
    var nt by remember { mutableIntStateOf(100) }
    var pt by remember { mutableFloatStateOf(0.5f) }
    var maxf by remember { mutableFloatStateOf(15f) }
    var alphaprior by remember { mutableFloatStateOf(0.5f) }
    var betaprior by remember { mutableFloatStateOf(0.5f) }
    var priorw by remember { mutableFloatStateOf(0.5f) }
    var postw by remember { mutableFloatStateOf(0.5f) }
    //var integral by remember { mutableDoubleStateOf(0.0) }



    var prior1 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var prior2 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var prior by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }


    var post1 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var post2 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var mydata by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }



    LaunchedEffect(ns, ps,alphaprior,betaprior, priorw) {

        val xs = ps * ns

        val prior1Def = async {
            val betadist1 = betapdf((xs + alphaprior).toDouble(), ((ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist1(p).toFloat())
            }
        }
        val prior2Def = async {
            val betadist1 = betapdf( alphaprior.toDouble(),  betaprior.toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist1(p).toFloat())
            }
        }


        prior1 = prior1Def.await()
        prior2 = prior2Def.await()
        prior = prior1.zip(prior2).map {
            DefaultPoint(
                x = it.first.x,
                y = (priorw * it.first.y + (1 - priorw) * it.second.y)
            )
        }
    }


    LaunchedEffect(ns, ps, nt, pt,alphaprior,betaprior,priorw) {

        val xs = ps * ns
        val xt = pt * nt



        val postwUnweighted1 = priorw * exp(
            lnBeta((xt + xs + alphaprior).toDouble(), ((nt - xt) + (ns - xs) + betaprior).toDouble())
                    - lnBeta((xs + alphaprior).toDouble(), ((ns - xs) + betaprior).toDouble())
        )
        val postwUnweighted2 = (1.0-priorw) * exp(
            lnBeta((xt + alphaprior).toDouble(), (nt - xt + betaprior).toDouble())
                    - lnBeta(alphaprior.toDouble(), betaprior.toDouble())
        )
        postw = (postwUnweighted1 / (postwUnweighted1 + postwUnweighted2)).toFloat()


        val post1Def = async(Dispatchers.Default) {
            val betadist = betapdf((xt + xs + alphaprior).toDouble(), (nt - xt + (ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist(p).toFloat())
            }
        }

        val post2Def = async(Dispatchers.Default) {
            val betadist2 = betapdf((xt + alphaprior).toDouble(), (nt - xt + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist2(p).toFloat())
            }
        }

        post1 = post1Def.await()
        post2 = post2Def.await()

        launch (Dispatchers.Default) {
            mydata = post1.zip(post2).map {
                DefaultPoint(
                    x = it.first.x,
                    y = ((postw * it.first.y + (1 - postw) * it.second.y))
                )
            }
        }
    }


    Column(modifier=Modifier.padding(10.dp).fillMaxSize()) {

        Text(modifier= Modifier.fillMaxWidth().padding(10.dp),
            textAlign = TextAlign.Center,
            text="Robust mixture prior",

            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))

        Text(text="Model",
            style= MaterialTheme.typography.headlineSmall)

        Text(
            modifier= Modifier.fillMaxWidth().padding(10.dp),
            text="The robust mixture prior is a mixture of a vague prior and a more informative prior which use information from the source study",
            style= MaterialTheme.typography.bodyMedium
        )


        Text(
            style=MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic) ,
            text = "M0: p~Beta(α, β)\nM1: p~Beta(xs+α, ns-xs+β)\n" +
                    "Robust mixture prior: p~w*M1+(1-w)*M0"
        )

        Row(horizontalArrangement = Arrangement.SpaceBetween) {

            Column(Modifier.weight(0.25f).padding(10.dp)) {
                Text(
                    modifier= Modifier.fillMaxWidth().padding(10.dp),
                    text="Source study",
                    style= MaterialTheme.typography.headlineSmall
                )
                Row {
                    TextField(
                        modifier = Modifier.padding(10.dp),
                        value = ns.toString(),
                        onValueChange = { ns = it.toIntOrNull() ?: ns },
                        label = { Text("n (source)") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Success rate in source study: ${round(1000 * ps) / 10}%")
                        Slider(
                            enabled = true,
                            value = ps,
                            onValueChange = { newValue ->
                                ps = newValue
                            },
                            valueRange = 0f..1f,
                        )
                    }
                }

            }


                Column(Modifier.weight(0.25f).padding(10.dp)) {
                    Text(
                        modifier= Modifier.fillMaxWidth().padding(10.dp),
                        text="Target study",
                        style= MaterialTheme.typography.headlineSmall
                    )
                    Row {
                        TextField(
                            modifier = Modifier.padding(10.dp),
                            value = nt.toString(),
                            onValueChange = { nt = it.toIntOrNull() ?: nt },
                            label = { Text("n (target)") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            )
                        )
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "Success rate in target study: ${round(1000 * pt) / 10}%")
                                Slider(

                                    enabled = true,
                                    value = pt,
                                    onValueChange = { newValue ->
                                        pt = newValue
                                    },
                                    valueRange = 0f..1f,
                                )
                            }
                        }

            }


        }

        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(0.25f).fillMaxWidth(0.3f).padding(10.dp)) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    text = "Priors",
                    style = MaterialTheme.typography.headlineSmall
                )



                Row(modifier = Modifier.fillMaxWidth(0.4f)) {
                    Column(modifier = Modifier.weight(0.5f).padding(10.dp)) {
                        Text(text = "alpha (prior): ${(round(10 * alphaprior) / 10f).toString().take(3)}")
                        Slider(

                            enabled = true,
                            value = log10(alphaprior),
                            onValueChange = { newValue ->
                                alphaprior = 10f.pow(newValue)
                            },
                            valueRange = log10(0.01f)..log10(100.0f),
                        )

                    }


                    Column(modifier = Modifier.weight(0.5f).padding(10.dp)) {
                        Text(text = "beta (prior): ${(round(10 * betaprior) / 10f).toString().take(3)}")
                        Slider(

                            enabled = true,
                            value = log10(betaprior),
                            onValueChange = { newValue ->
                                betaprior = 10f.pow(newValue)
                            },
                            valueRange = log10(0.01f)..log10(100.0f),
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(0.5f).fillMaxWidth(0.3f).padding(10.dp)) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    text = "Mixture weight",
                    style = MaterialTheme.typography.headlineSmall
                )
                Row {
                    Column(modifier = Modifier.weight(0.25f).padding(10.dp)) {
                        Text(
                            text = "Weigts on priors (use source / dont use source): ${round(1000 * priorw) / 10}% / ${
                                round(
                                    1000 * (1 - priorw)
                                ) / 10
                            }%"
                        )
                        Slider(
                            enabled = true,
                            value = priorw,
                            onValueChange = { newValue ->
                                priorw = newValue
                            },
                            valueRange = 0f..1f,
                        )

                    }

                    Text(
                        "Weights on posterior (use source / dont use source): ${round(1000 * postw) / 10}% / ${
                            round(
                                1000 * (1 - postw)
                            ) / 10
                        }%", modifier = Modifier.padding(10.dp)
                    )
                }

            }


        }
        Row(horizontalArrangement = Arrangement.Start) {
            TextField(
                modifier = Modifier.padding(10.dp).fillMaxWidth(0.2f),
                value = maxf.toString(),
                onValueChange = { maxf = it.toFloatOrNull() ?: maxf },
                label = { Text("Max y-axis value") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )

        }

        val legends= listOf("Mixture","No Source","With Source")

        val colors = generateHueColorPalette(legends.size)

        Row {

            Column(modifier = Modifier.weight(0.5f)) {

                ChartLayout(
                    title = { Text(text="Prior densities",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge,) },
                    legend = { Legend(legends, colors) },
                    legendLocation = LegendLocation.BOTTOM
                ) {
                    XYGraph(
                        modifier = Modifier.weight(0.5f).fillMaxSize(),
                        xAxisModel = FloatLinearAxisModel(0f..1f),
                        yAxisModel = FloatLinearAxisModel(0f..maxf),
                        xAxisTitle = "Probability parameter",
                        xAxisLabels = { value -> (round(10 * value) / 10f).toString().take(3) },
                        yAxisTitle = "Density",
                    ) {
                        LinePlot(
                            prior,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[0]))
                        )
                        LinePlot(
                            prior1,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[2]))
                        )
                        LinePlot(
                            prior2,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[1]))
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(0.5f)) {

                ChartLayout(
                    title = { Text(text="Posterior distributions",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge,) },
                    legend = { Legend(legends, colors) },
                    legendLocation = LegendLocation.BOTTOM
                ) {
                XYGraph(
                    modifier = Modifier.weight(0.5f).fillMaxSize(),
                    xAxisModel = FloatLinearAxisModel(0f..1f),
                    yAxisModel = FloatLinearAxisModel(0f..maxf),


                    xAxisTitle = "Probability parameter",
                    xAxisLabels = { value -> (round(10 * value) / 10f).toString().take(3) },
                    yAxisTitle = "Density",

                    ) {

                    LinePlot(
                        data = mydata,
                        lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[0]))
                    )
                    LinePlot(
                        data = post1,
                        lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[2 ]))
                    )
                    LinePlot(
                        data = post2,
                        lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[1]),
                            )
                    )
                }
            }
            }
        }
    }
}


