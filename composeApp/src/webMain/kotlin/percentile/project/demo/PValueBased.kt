package percentile.project.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import kotlinx.coroutines.async
import kotlin.math.*

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun PValueBased() {
    var ns by remember { mutableIntStateOf(100) }
    var ps by remember { mutableFloatStateOf(0.5f) }
    var nt by remember { mutableIntStateOf(100) }
    var pt by remember { mutableFloatStateOf(0.5f) }
    var maxf by remember { mutableFloatStateOf(15f) }
    var alphaprior by remember { mutableFloatStateOf(0.5f) }
    var betaprior by remember { mutableFloatStateOf(0.5f) }
    var lambda by remember { mutableFloatStateOf(0.5f) }
    var kappa by remember { mutableFloatStateOf(0.5f) }
    var weight by remember { mutableFloatStateOf(0.5f) }
    //var integral by remember { mutableDoubleStateOf(0.0) }


    var prior by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var prior0 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var prior1 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var post by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var post0 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var post1 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }





    LaunchedEffect(ns, ps, nt, pt,alphaprior,betaprior,kappa,lambda) {

        val xs = ps * ns
        val xt = pt * nt


        val p1=((xs+alphaprior)/(ns+alphaprior+betaprior)).toDouble()
        val theta1=ln(p1/(1-p1))
        val v1=1/((ns+betaprior+alphaprior)*p1*(1-p1))


        val p2=((xt+alphaprior)/(nt+alphaprior+betaprior)).toDouble()
        val theta2=ln(p2/(1-p2))
        val v2=1/((nt+betaprior+alphaprior)*p2*(1-p2))

        val mean=theta1-theta2
        val sd=sqrt(v1+v2)

        val pval = 1.0-normal_cdf( ln(lambda.toDouble()).absoluteValue,mean ,sd)+normal_cdf(-ln(lambda.toDouble()).absoluteValue,mean ,sd)
        weight = exp(kappa*ln(1-pval)/(1-pval)).toFloat()

        val priorDef = async {
            val betadist1 = betapdf((weight*xs + alphaprior).toDouble(), (weight*(ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist1(p).toFloat())
            }
        }


        val prior0Def = async {
            val betadist1 = betapdf(( alphaprior).toDouble(), ( + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist1(p).toFloat())
            }
        }

        val prior1Def = async {
            val betadist1 = betapdf((xs + alphaprior).toDouble(), ((ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist1(p).toFloat())
            }
        }


        val postDef = async {
            val betadist1 = betapdf((xt+weight*xs + alphaprior).toDouble(), (nt-xt+weight*(ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist1(p).toFloat())
            }
        }
        val post0Def = async {
            val betadist1 = betapdf((xt + alphaprior).toDouble(), (nt-xt + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist1(p).toFloat())
            }
        }
        val post1Def = async {
            val betadist1 = betapdf((xt +xs+ alphaprior).toDouble(), (nt-xt+(ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x=p.toFloat(),betadist1(p).toFloat())
            }
        }


        prior= priorDef.await()
        prior0= prior0Def.await()
        prior1= prior1Def.await()
        post= postDef.await()
        post0= post0Def.await()
        post1= post1Def.await()
    }


    Column(modifier=Modifier.padding(10.dp).fillMaxSize()) {

        Text(modifier= Modifier.fillMaxWidth().padding(10.dp),
            textAlign = TextAlign.Center,
            text="P-value based power prior",

            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))

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

        Text("weight=${round(100.0*weight)/100.0}")
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(0.33f).padding(10.dp)) {
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

            Column(modifier = Modifier.weight(0.33f).padding(10.dp)) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    text = "Kappa",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(text="Current kappa: ${(round(1000 * (kappa.toDouble()).absoluteValue)) / 1000.0}")
                Row {
                    Column(modifier = Modifier.weight(0.33f).padding(10.dp)) {

                        Slider(
                            enabled = true,
                            value = kappa,
                            onValueChange = { newValue ->
                                kappa = newValue
                            },
                            valueRange = 0f..10f,
                        )

                    }

                }

            }

            Column(modifier = Modifier.weight(0.5f).fillMaxWidth(0.3f).padding(10.dp)) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    text = "Lambda (Odds Ratio threshold for similarity)",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(text="Current OR threshold: ${(round(1000 * ((lambda.toDouble()).absoluteValue)) / 1000.0).toString().take(5)}")
                Row {
                    Column(modifier = Modifier.weight(0.25f).padding(10.dp)) {

                        Slider(
                            enabled = true,
                            value = lambda,
                            onValueChange = { newValue ->
                                lambda = newValue
                            },
                            valueRange = 0f..8f,
                        )

                    }

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
                    title = { Text(text="Prior distributions",
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
                            data = prior,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[0 ]))
                        )
                        LinePlot(
                            data = prior0,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[1 ]))
                        )
                        LinePlot(
                            data = prior1,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[2 ]))
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
                            data = post,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[0 ]))
                        )
                        LinePlot(
                            data = post0,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[1 ]))
                        )
                        LinePlot(
                            data = post1,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[2 ]))
                        )
                    }
                }
            }
        }
    }
}