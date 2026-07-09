package percentile.project.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleColorManual
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.themes.elementText
import org.jetbrains.letsPlot.themes.theme
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/**
 * A Composable that implements the Robust Mixture Prior borrowing model UI.
 * This model allows borrowing information from a source dataset into a target dataset
 * using a mixture of informative and non-informative priors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobustMixture() {
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
    var priorw by remember { mutableFloatStateOf(0.5f) }
    var postw by remember { mutableFloatStateOf(0.5f) }
    
    // Data for plots
    var prior1 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var prior2 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var prior by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    var post1 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var post2 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var mydata by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Re-calculate prior distributions when parameters change
    LaunchedEffect(ns, ps,alphaprior,betaprior, priorw) {
        val priorDef = async(Dispatchers.Default) {
            val dist = BorrowingModels.robustMixturePrior(ns, ps, alphaprior, betaprior, priorw)
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to dist(p).toFloat())
            }
        }
        val prior1Def = async(Dispatchers.Default) {
            val xs = ps * ns
            val betadist1 = betapdf((xs + alphaprior).toDouble(), ((ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist1(p).toFloat())
            }
        }
        val prior2Def = async(Dispatchers.Default) {
            val betadist1 = betapdf( alphaprior.toDouble(),  betaprior.toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist1(p).toFloat())
            }
        }

        prior = priorDef.await()
        prior1 = prior1Def.await()
        prior2 = prior2Def.await()
    }

    // Re-calculate posterior distributions when parameters change
    LaunchedEffect(ns, ps, nt, pt,alphaprior,betaprior,priorw) {
        val xt = pt * nt
        val xs = ps * ns

        // Calculate posterior weight for the mixture
        val postwUnweighted1 = priorw * exp(
            lnBeta((xt + xs + alphaprior).toDouble(), ((nt - xt) + (ns - xs) + betaprior).toDouble())
                    - lnBeta((xs + alphaprior).toDouble(), ((ns - xs) + betaprior).toDouble())
        )
        val postwUnweighted2 = (1.0-priorw) * exp(
            lnBeta((xt + alphaprior).toDouble(), (nt - xt + betaprior).toDouble())
                    - lnBeta(alphaprior.toDouble(), betaprior.toDouble())
        )
        postw = (postwUnweighted1 / (postwUnweighted1 + postwUnweighted2)).toFloat()

        val postDef = async(Dispatchers.Default) {
            val dist = BorrowingModels.robustMixturePosterior(ns, ps, nt, pt, alphaprior, betaprior, priorw)
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to dist(p).toFloat())
            }
        }

        val post1Def = async(Dispatchers.Default) {
            val betadist = betapdf((xt + xs + alphaprior).toDouble(), (nt - xt + (ns - xs) + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist(p).toFloat())
            }
        }

        val post2Def = async(Dispatchers.Default) {
            val betadist2 = betapdf((xt + alphaprior).toDouble(), (nt - xt + betaprior).toDouble())
            (1..999).map {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist2(p).toFloat())
            }
        }

        mydata = postDef.await()
        post1 = post1Def.await()
        post2 = post2Def.await()
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

        Row {

            Column(modifier = Modifier.weight(0.5f)) {
                val priorFigure = letsPlot() +
                        geomLine(data = mapOf("x" to prior.map { it["x"] }, "y" to prior.map { it["y"] }, "c" to List(prior.size) { legends[0] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to prior2.map { it["x"] }, "y" to prior2.map { it["y"] }, "c" to List(prior2.size) { legends[1] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to prior1.map { it["x"] }, "y" to prior1.map { it["y"] }, "c" to List(prior1.size) { legends[2] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        ggtitle("Prior densities") +
                        scaleColorManual(values=listOf("red", "blue", "green"), naValue = "gray", name = "") +
                        scaleYContinuous(limits = 0 to maxf) +
                        scaleXContinuous(name="Success rate in target study") +
                        theme(
                            plotTitle = elementText(size = 20, hjust = 0.5),
                            legendText = elementText(size = 15),
                            axisText = elementText(size = 15)
                        ).legendPositionBottom()

                PlotPanel(figure = priorFigure, modifier = Modifier.weight(0.5f).fillMaxSize())
            }

            Column(modifier = Modifier.weight(0.5f)) {
                val postFigure = letsPlot() +
                        geomLine(data = mapOf("x" to mydata.map { it["x"] }, "y" to mydata.map { it["y"] }, "c" to List(mydata.size) { legends[0] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to post2.map { it["x"] }, "y" to post2.map { it["y"] }, "c" to List(post2.size) { legends[1] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to post1.map { it["x"] }, "y" to post1.map { it["y"] }, "c" to List(post1.size) { legends[2] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        ggtitle("Posterior distributions") +
                        scaleColorManual(values=listOf("red", "blue", "green"), naValue = "gray", name = "") +
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
}


