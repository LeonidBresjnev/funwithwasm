package percentile.project.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleColorManual
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.themes.elementText
import org.jetbrains.letsPlot.themes.theme
import kotlin.math.*

/**
 * A Composable that implements the P-Value Based Borrowing model UI.
 * This model discounts the source data based on a p-value that measures 
 * the similarity between source and target data.
 */
@Composable
fun PValueBased() {
    // State for source data parameters
    var ns by remember { mutableIntStateOf(100) }
    var ps by remember { mutableFloatStateOf(0.5f) }
    
    // State for target data parameters
    var nt by remember { mutableIntStateOf(100) }
    var pt by remember { mutableFloatStateOf(0.5f) }
    
    // UI state
    var maxf by remember { mutableFloatStateOf(15f) }
    
    // Model parameters
    var alphaprior by remember { mutableFloatStateOf(0.5f) }
    var betaprior by remember { mutableFloatStateOf(0.5f) }
    var lambda by remember { mutableFloatStateOf(0.5f) }
    var kappa by remember { mutableFloatStateOf(0.5f) }
    var weight by remember { mutableFloatStateOf(0.5f) }
    
    // Data for plots
    var prior by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var prior0 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var prior1 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var post by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var post0 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var post1 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Re-calculate distributions when parameters change
    LaunchedEffect(ns, ps, nt, pt,alphaprior,betaprior,kappa,lambda) {
        val priorDef = async {
            val dist = BorrowingModels.pValueBasedPrior(ns, ps, nt, pt, alphaprior, betaprior, kappa, lambda)
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to dist(p).toFloat())
            }
        }


        val prior0Def = async {
            val betadist1 = betapdf(( alphaprior).toDouble(), ( + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist1(p).toFloat())
            }
        }

        val prior1Def = async {
            val xs = ps * ns
            val betadist1 = betapdf((xs + alphaprior).toDouble(), ((ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist1(p).toFloat())
            }
        }


        val postDef = async {
            val dist = BorrowingModels.pValueBasedPosterior(ns, ps, nt, pt, alphaprior, betaprior, kappa, lambda)
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to dist(p).toFloat())
            }
        }
        val post0Def = async {
            val xt = pt * nt
            val betadist1 = betapdf((xt + alphaprior).toDouble(), (nt-xt + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist1(p).toFloat())
            }
        }
        val post1Def = async {
            val xt = pt * nt
            val xs = ps * ns
            val betadist1 = betapdf((xt +xs+ alphaprior).toDouble(), (nt-xt+(ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist1(p).toFloat())
            }
        }


        prior= priorDef.await()
        prior0= prior0Def.await()
        prior1= prior1Def.await()
        post= postDef.await()
        post0= post0Def.await()
        post1= post1Def.await()

        weight = BorrowingModels.pValueBasedWeight(ns, ps, nt, pt, alphaprior, betaprior, kappa, lambda).toFloat()
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

        Row {
                val priorFigure = letsPlot() +
                        geomLine(data = mapOf("x" to prior.map { it["x"] }, "y" to prior.map { it["y"] }, "c" to List(prior.size) { legends[0] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to prior0.map { it["x"] }, "y" to prior0.map { it["y"] }, "c" to List(prior0.size) { legends[1] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to prior1.map { it["x"] }, "y" to prior1.map { it["y"] }, "c" to List(prior1.size) { legends[2] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        ggtitle("Prior distributions") +
                        scaleColorManual(values = listOf("red", "blue", "green"), name = "") +
                        scaleYContinuous(limits = 0 to maxf) +
                        scaleXContinuous(name="Success rate in target study") +
                        theme(
                            plotTitle = elementText(size = 20, hjust = 0.5),
                            legendText = elementText(size = 15),
                            axisText = elementText(size = 15)
                        ).legendPositionBottom()

                PlotPanel(figure = priorFigure, modifier = Modifier.weight(0.5f).fillMaxSize())


                val postFigure = letsPlot() +
                        geomLine(data = mapOf(
                            "x" to post.map { it["x"] },
                            "y" to post.map { it["y"] },
                            "c" to List(post.size) { legends[0] }), size = 2.0) {
                            x = "x"
                            y = "y"
                            color = "c"
                        } +
                        geomLine(data = mapOf("x" to post0.map { it["x"] }, "y" to post0.map { it["y"] }, "c" to List(post0.size) { legends[1] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to post1.map { it["x"] }, "y" to post1.map { it["y"] }, "c" to List(post1.size) { legends[2] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        ggtitle("Posterior distributions") +
                        scaleColorManual(values = listOf("red", "blue", "green"), name = "") +
                        scaleYContinuous(limits = 0 to maxf) +
                        scaleXContinuous(name="Success rate in target study") +
                        theme(
                            plotTitle = elementText(size = 20, hjust = 0.5),
                            legendText = elementText(size = 15),
                            axisText = elementText(size = 15)
                        ).legendPositionBottom()

                PlotPanel(figure = postFigure, modifier = Modifier.weight(0.5f).fillMaxSize())

        }
    }
}