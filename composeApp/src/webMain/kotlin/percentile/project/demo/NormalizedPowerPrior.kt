package percentile.project.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.jetbrains.letsPlot.Figure
import org.jetbrains.letsPlot.geom.geomLine
import org.jetbrains.letsPlot.label.ggtitle
import org.jetbrains.letsPlot.letsPlot
import org.jetbrains.letsPlot.scale.scaleColorManual
import org.jetbrains.letsPlot.scale.scaleXContinuous
import org.jetbrains.letsPlot.scale.scaleYContinuous
import org.jetbrains.letsPlot.themes.elementText
import org.jetbrains.letsPlot.themes.theme
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

// import org.jetbrains.letsPlot.compose.PlotPanel
/*
fun betaFunction(x: Double, y: Double, steps: Int = 100): Double {
    val h = 1.0 / steps
    return (1..<steps).sumOf {
        val p=it.toDouble()*h
        p.pow(x-1.0) * (1.0 - p).pow(y-1)*h
    }
}*/

@Composable
expect fun PlotPanel(figure: Figure, modifier: Modifier)


/**
 * A Composable that implements the Normalized Power Prior (NPP) borrowing model UI.
 * This model discounts the source data by a factor w, which is assigned a prior distribution.
 */
@OptIn(ExperimentalWasmJsInterop::class, ExperimentalMaterial3Api::class)
@Composable
fun NormalizedPowerPrior() {

    // State for source and target data parameters
    var ns by remember { mutableIntStateOf(100) }
    var ps by remember { mutableFloatStateOf(0.5f) }
    var nt by remember { mutableIntStateOf(100) }
    var pt by remember { mutableFloatStateOf(0.5f) }
    
    // UI state
    var maxf by remember { mutableFloatStateOf(15f) }
    
    // Model parameters
    var alphaprior by remember { mutableFloatStateOf(0.5f) }
    var betaprior by remember { mutableFloatStateOf(0.5f) }
    var alphaw by remember { mutableFloatStateOf(1f) }
    var betaw by remember { mutableFloatStateOf(1f) }
    var integral by remember { mutableDoubleStateOf(0.0) }

    // Data for plots
    var marginalprior by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var marginalprob by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var probw0 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var probw1 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var wprior by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var wpost by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var priorw0 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var priorw1 by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    //  var integral by remember { mutableStateOf(0.0) }
    /*
        LaunchedEffect(Unit){
            js("""

                const jStat = require('jstat');
                const result = jStat.beta.pdf(0.5, 2, 5); // PDF of Beta(2,5) at x = 0.
    alert('Hello from Kotlin/Wasm via js(). '+result);
            """.trimIndent())
        }*/


    LaunchedEffect(ns, ps,alphaprior,betaprior,alphaw,betaw) {
        val xs = ps * ns
        val resultDef = async(context= Dispatchers.Default) {
            val dist = BorrowingModels.nppPrior(ns, ps, alphaprior, betaprior, alphaw, betaw)
            (0 until 99).map {
                val p = (it+1).toDouble() / 100.0
                mapOf("x" to p.toFloat(), "y" to dist(p).toFloat())
            }
        }

        val wpriorDef = async(context= Dispatchers.Default) {
            val wbetacoeff = lnBeta(alphaw.toDouble(), betaw.toDouble())
            (0 until 999).map { widx ->
                val w = (widx+1).toDouble() / 1000.0
                val logf = (alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w)- wbetacoeff
                val pdf = exp(logf)
                mapOf("x" to w.toFloat(), "y" to pdf.toFloat())
            }
        }

        val propw0Def = async(context= Dispatchers.Default) {
            val betadist0 = betapdf((alphaprior).toDouble(), (betaprior).toDouble())
            List(999) {
                val p = (it+1).toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist0(p).toFloat())
            }
        }
        val propw1Def = async(context= Dispatchers.Default) {
            val betadist1 = betapdf((alphaprior+xs).toDouble(), (betaprior +ns -xs).toDouble())
            List(999) {
                val p = (it+1).toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist1(p).toFloat())
            }
        }
        priorw0 = propw0Def.await()
        priorw1 = propw1Def.await()
        wprior = wpriorDef.await()

        marginalprior  = resultDef.await()
    }

    LaunchedEffect(ns, ps, nt, pt,alphaprior,betaprior,alphaw,betaw) {
        val xs=ps*ns
        val xt=pt*nt
        val stepsize=1000.0
        val stepsizeinv=1/stepsize

        val grandintegralDef = async(context= Dispatchers.Default) {
            val f = List(999) {
                val w = (it+1).toDouble() * stepsizeinv
                return@List exp(
                    lnBeta((xt + w * xs + alphaprior), nt - xt + w * (ns - xs) + betaprior) -
                            lnBeta(w * xs + alphaprior, w * (ns - xs) + betaprior)+
                            (alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w) -
                            lnBeta(alphaw.toDouble(), betaw.toDouble())
                )
            }

            val grandIntegral = 0.5 *(f.zipWithNext().sumOf { it2 ->
                (it2.first + it2.second) * 0.001
            }  + (3.0*f.first()-f[1]+ 3.0*f.last()-f[f.lastIndex-1])*0.001)
            return@async grandIntegral

        }
        val resultDef = async(context= Dispatchers.Default) {
            val dist = BorrowingModels.nppPosterior(ns, ps, nt, pt, alphaprior, betaprior, alphaw, betaw)
            List(101) {
                val p = it.toDouble() / 100.0
                mapOf("x" to p.toFloat(), "y" to dist(p).toFloat())
            }
        }

        val resultwDef = async(context= Dispatchers.Default) {
            List(999) { widx ->
                val w = (widx+1).toDouble() / 1000.0
                val logf =(lnBeta(xt+xs*w+alphaprior,(nt-xt)+(ns-xs)*w+betaprior) -
                        lnBeta(xs*w+alphaprior,(ns-xs)*w+betaprior)+ (alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w) -
                        lnBeta(alphaw.toDouble(), betaw.toDouble()))
                val pdf = exp(logf)
                return@List pdf
            }
        }

        val propw0Def = async(context= Dispatchers.Default) {
            val betadist0 = betapdf((alphaprior+xt).toDouble(), (betaprior+nt -xt).toDouble())
            List(999) {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist0(p).toFloat())
            }
        }
        val propw1Def = async(context= Dispatchers.Default) {
            val betadist1 = betapdf((alphaprior+xt+xs).toDouble(), (betaprior+nt -xt +ns -xs).toDouble())
            List(999) {
                val p = it.toDouble() / 1000.0
                mapOf("x" to p.toFloat(), "y" to betadist1(p).toFloat())
            }
        }



        val grandintegral = grandintegralDef.await()
        val resultunscaled = resultDef.await()
        val resultwunscaled = resultwDef.await()

        marginalprob = resultunscaled
        wpost= resultwunscaled.mapIndexed { idx, it ->
            mapOf("x" to ((idx+1).toDouble()/1000.0).toFloat(), "y" to (it/grandintegral).toFloat())
        }


        probw0 = propw0Def.await()
        probw1 = propw1Def.await()

        /*
                val result = resultdeffered.await()
                val resultw = resultdefferedw.await()
                result.fold(
                    onSuccess = { codes ->
                        val marginal = codes[0]

                        val integral = marginal.sumOf {  it/marginal.size }
                        if (  (integral-1.0).absoluteValue>0.2) {
                            showAlert=true
                        }
                        marginalprob = marginal.mapIndexed { idx, it ->
                            DefaultPoint(x=(idx.toDouble()/marginal.size).toFloat(),it.toFloat())
                        }

                        probw0 = codes[1].mapIndexed { idx, it ->
                            DefaultPoint(x=(idx.toDouble()/codes[1].size).toFloat(),it.toFloat())
                        }
                        probw1 = codes[2].mapIndexed { idx, it ->
                            DefaultPoint(x=(idx.toDouble()/codes[2].size).toFloat(),it.toFloat())
                        }


                    },
                    onFailure = { error ->
                        println("Error loading ICD codes: ${error.message}")
                    }
                )

                resultw.fold(
                    onSuccess = { codes ->
                        val integral = codes[0].sumOf { it/codes[0].size }
                        if (  (integral-1.0).absoluteValue>0.2) {
                            showAlert=true
                        }
                        wpdf = codes[0].mapIndexed { idx,it ->
                            DefaultPoint(x=(idx.toDouble()/codes[0].size).toFloat(),it.toFloat())
                        }

                    },
                    onFailure = { error ->
                        println("Error loading ICD codes: ${error.message}")
                    }
                )*/
    }
    /*
        if (showAlert) {
            BasicAlertDialog(
                onDismissRequest = { showAlert = false },

            ) {
                Text("Warning: The integral of the weight density deviates significantly from 1.!")
                Button(onClick = { showAlert = false }) {
                    Text("OK")
                }
            }
        }*/

    Column(modifier=Modifier.padding(10.dp).fillMaxSize()) {
        Text(modifier=Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text="Normalized Power Prior Demo",

            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
        Row(modifier=Modifier.weight(0.2f).fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween) {

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

        // Text(text="integral=$integral"     )
        Row(modifier=Modifier.weight(0.1f).fillMaxWidth(0.5f)) {

            Column(modifier = Modifier.weight(0.4f)) {
                Text(text="Prior parameters for success probability")
                Row {
                    Column(modifier = Modifier.weight(0.5f).padding(10.dp)) {
                        Text(text = "Alpha: ${(round(10 * alphaprior) / 10f).toString().take(3)}")
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
                        Text(text = "Beta: ${(round(10 * betaprior) / 10f).toString().take(3)}")
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
            Column(modifier = Modifier.weight(0.4f)) {
                Text(text="Prior parameters for the weight")
                Row {
                    Column(modifier = Modifier.weight(0.5f).padding(10.dp)) {
                        Text(text = "Alpha: ${(round(10 * alphaw) / 10f).toString().take(3)}")
                        Slider(

                            enabled = true,
                            value = log10(alphaw),
                            onValueChange = { newValue ->
                                alphaw = 10f.pow(newValue)
                            },
                            valueRange = log10(0.01f)..log10(100.0f),
                        )

                    }


                    Column(modifier = Modifier.weight(0.5f).padding(10.dp)) {
                        Text(text = "Beta: ${(round(10 * betaw) / 10f).toString().take(3)}")
                        Slider(

                            enabled = true,
                            value = log10(betaw),
                            onValueChange = { newValue ->
                                betaw = 10f.pow(newValue)
                            },
                            valueRange = log10(0.01f)..log10(100.0f),
                        )

                    }
                }
            }
            TextField(
                modifier = Modifier.weight(0.3f).padding(5.dp).fillMaxWidth(0.2f),
                value = maxf.toString(),
                onValueChange = { maxf = it.toFloatOrNull()?.run { if(this>0.0f) this else maxf  } ?: maxf },
                label = { Text("Max y-axis value") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                )
            )
        }
        // Text("integral=$integral")





        val legend = listOf("Marginal posterior density",
            "density when conditioning on weight=0",
            "density when conditioning on weight=1")

        Row(modifier=Modifier.weight(0.5f).fillMaxSize()) {
            Column(modifier = Modifier.weight(0.5f).fillMaxSize()) {
                val marginalPriorFigure = letsPlot() +
                        geomLine(data = mapOf("x" to marginalprior.map { it["x"] }, "y" to marginalprior.map { it["y"] }, "c" to List(marginalprior.size) { legend[0] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to priorw0.map { it["x"] }, "y" to priorw0.map { it["y"] }, "c" to List(priorw0.size) { legend[1] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to priorw1.map { it["x"] }, "y" to priorw1.map { it["y"] }, "c" to List(priorw1.size) { legend[2] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        ggtitle("Marginal prior density of the probability parameter") +
                        scaleColorManual(values = listOf("red", "green", "blue"), name = "") +
                        scaleYContinuous(limits = 0 to maxf) +
                        scaleXContinuous(name="Success rate in target study") +
                        theme(
                            plotTitle = elementText(size = 20, hjust = 0.5),
                            legendText = elementText(size = 15),
                            axisText = elementText(size = 15)
                        ).legendPositionBottom()

                PlotPanel(figure = marginalPriorFigure, modifier = Modifier.weight(0.5f).fillMaxSize())

                val wpriorFigure = letsPlot() +
                        geomLine(data = mapOf("x" to wprior.map { it["x"] }, "y" to wprior.map { it["y"] }), size = 2.0) { x = "x"; y = "y" } +
                        ggtitle("Marginal prior density of the weight parameter") +
                        scaleYContinuous(limits = 0 to maxf) +
                        scaleXContinuous(name="Weight") +
                        theme(
                            plotTitle = elementText(size = 20, hjust = 0.5),
                            axisText = elementText(size = 15)
                        )

                PlotPanel(figure = wpriorFigure, modifier = Modifier.weight(0.5f).fillMaxSize())
            }
            Column(modifier = Modifier.weight(0.5f).fillMaxSize()) {
                val marginalProbFigure = letsPlot() +
                        geomLine(data = mapOf("x" to marginalprob.map { it["x"] }, "y" to marginalprob.map { it["y"] }, "c" to List(marginalprob.size) { legend[0] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to probw0.map { it["x"] }, "y" to probw0.map { it["y"] }, "c" to List(probw0.size) { legend[1] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        geomLine(data = mapOf("x" to probw1.map { it["x"] }, "y" to probw1.map { it["y"] }, "c" to List(probw1.size) { legend[2] }), size = 2.0) { x = "x"; y = "y"; color = "c" } +
                        ggtitle("Marginal posterior density of the probability parameter") +
                        scaleColorManual(values = listOf("red", "green", "blue"), name = "") +
                        scaleYContinuous(limits = 0 to maxf) +
                        scaleXContinuous(name="Success rate in target study") +
                        theme(
                            plotTitle = elementText(size = 20, hjust = 0.5),
                            legendText = elementText(size = 15),
                            axisText = elementText(size = 15)
                        ).legendPositionBottom()

                PlotPanel(figure = marginalProbFigure, modifier = Modifier.weight(0.5f).fillMaxSize())

                val wpostFigure = letsPlot() +
                        geomLine(data = mapOf("x" to wpost.map { it["x"] }, "y" to wpost.map { it["y"] }), size = 2.0) { x = "x"; y = "y" } +
                        ggtitle("Marginal posterior density of the weight parameter") +
                        scaleYContinuous(limits = 0 to maxf) +
                        scaleXContinuous(name="Weight") +
                        theme(
                            plotTitle = elementText(size = 20, hjust = 0.5),
                            axisText = elementText(size = 15)
                        )

                PlotPanel(figure = wpostFigure, modifier = Modifier.weight(0.5f).fillMaxSize())
            }
        }
    }
}