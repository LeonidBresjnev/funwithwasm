package percentile.project.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

/*
fun betaFunction(x: Double, y: Double, steps: Int = 100): Double {
    val h = 1.0 / steps
    return (1..<steps).sumOf {
        val p=it.toDouble()*h
        p.pow(x-1.0) * (1.0 - p).pow(y-1)*h
    }
}*/

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalWasmJsInterop::class, ExperimentalMaterial3Api::class)
@Composable
fun NormalizedPowerPrior() {

    var ns by remember { mutableIntStateOf(100) }
    var ps by remember { mutableFloatStateOf(0.5f) }
    var nt by remember { mutableIntStateOf(100) }
    var pt by remember { mutableFloatStateOf(0.5f) }
    var maxf by remember { mutableFloatStateOf(15f) }
    var alphaprior by remember { mutableFloatStateOf(0.5f) }
    var betaprior by remember { mutableFloatStateOf(0.5f) }
    var alphaw by remember { mutableFloatStateOf(1f) }
    var betaw by remember { mutableFloatStateOf(1f) }
    var integral by remember { mutableDoubleStateOf(0.0) }

    //var showAlert by remember { mutableStateOf(false) }

    var marginalprior by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var marginalprob by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var probw0 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var probw1 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var wprior by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var wpost by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var priorw0 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
    var priorw1 by remember { mutableStateOf<List<DefaultPoint<Float,Float>>>(emptyList()) }
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

        val stepsize=1000.0
        val stepsizeinv=1/stepsize

        //println("$ns $xs $nt $xt $grandintegral")
        val resultDef = async(context= Dispatchers.Default) {
            val wbetacoeff = lnBeta(alphaw.toDouble(), betaw.toDouble())
            List(99) {
                val p = (it+1).toDouble() / 100.0
                val y = List(999) { widx ->
                    val w = (widx+1).toDouble() * stepsizeinv

                    val c = lnBeta(w * xs + alphaprior, w * (ns - xs) + betaprior)
                    val pdf = ( exp(
                            ln(p) * (w * xs + alphaprior - 1.0) + ln(1.0 - p) * (w * (ns - xs) + betaprior - 1.0) - c +
                                    (alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w) - wbetacoeff
                        ))
                    return@List pdf
                }

                integral =  0.5 *(y.zipWithNext().sumOf { it2 ->
                    (it2.first + it2.second) * 0.001
                }  + (3.0*y.first()-y[1]+ 3.0*y.last()-y[y.lastIndex-1])*0.001)


                return@List DefaultPoint(x=p.toFloat(),integral.toFloat())
            }



        }

        val wpriorDef = async(context= Dispatchers.Default) {
            val wbetacoeff = lnBeta(alphaw.toDouble(), betaw.toDouble())
            List(999) { widx ->
                val w = (widx+1).toDouble() / 1000.0
                val logf = (alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w)- wbetacoeff
                val pdf = exp(logf)
                return@List DefaultPoint(x = w.toFloat(), y = pdf.toFloat())
            }
        }

        val propw0Def = async(context= Dispatchers.Default) {
            val betadist0 = betapdf((alphaprior).toDouble(), (betaprior).toDouble())
            List(999) {
                val p = (it+1).toDouble() / 1000.0
                DefaultPoint(x = p.toFloat(), y = betadist0(p).toFloat())
            }
        }
        val propw1Def = async(context= Dispatchers.Default) {
            val betadist1 = betapdf((alphaprior+xs).toDouble(), (betaprior +ns -xs).toDouble())
            List(999) {
                val p = (it+1).toDouble() / 1000.0
                DefaultPoint(x = p.toFloat(), y = betadist1(p).toFloat())
            }
        }
        priorw0 = propw0Def.await()
        priorw1 = propw1Def.await()
        wprior = wpriorDef.await()

        marginalprior  = resultDef.await()
        integral = 0.5 *(marginalprior.zipWithNext().sumOf {
             (it.first.y.toDouble() + it.second.y.toDouble()) * 0.01
        } + (3.0*marginalprior.first().y-marginalprior[1].y+ 3.0*marginalprior.last().y-marginalprior[marginalprior.lastIndex-1].y)*0.01)
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

            integral = 0.5 *(f.zipWithNext().sumOf { it2 ->
                 (it2.first + it2.second) * 0.001
            }  + (3.0*f.first()-f[1]+ 3.0*f.last()-f[f.lastIndex-1])*0.001)
            return@async integral

        }
        //println("$ns $xs $nt $xt $grandintegral")
        val resultDef = async(context= Dispatchers.Default) {
             List(101) {
                val p = it.toDouble() / 100.0
                val y = List(999) { widx ->
                    val w = (widx+1).toDouble() * 0.001

                    val c = lnBeta(w * xs + alphaprior, w * (ns - xs) + betaprior)
                    val y = exp(ln(p)*(xt + w * xs + alphaprior - 1.0) + ln(1.0 - p)*((nt - xt) +
                            w * (ns - xs) + betaprior - 1.0) - c +
                            (alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w) -
                            lnBeta(alphaw.toDouble(), betaw.toDouble()))
                    return@List y
                }

                 integral = 0.5 * (y.zipWithNext().sumOf { it2 ->
                      (it2.first + it2.second) * 0.001
                 }  + (3.0*y.first()-y[1]+ 3.0*y.last()-y[y.lastIndex-1])*0.001)

                return@List integral
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
                DefaultPoint(x = p.toFloat(), y = betadist0(p).toFloat())
            }
        }
        val propw1Def = async(context= Dispatchers.Default) {
            val betadist1 = betapdf((alphaprior+xt+xs).toDouble(), (betaprior+nt -xt +ns -xs).toDouble())
            List(999) {
                val p = it.toDouble() / 1000.0
                DefaultPoint(x = p.toFloat(), y = betadist1(p).toFloat())
            }
        }



        val grandintegral = grandintegralDef.await()
        val resultunscaled = resultDef.await()
        val resultwunscaled = resultwDef.await()

        marginalprob= resultunscaled
            .mapIndexed { idx, it ->
            DefaultPoint(x=(idx.toDouble()/100.0).toFloat(),(it/grandintegral).toFloat())
        }

        wpost= resultwunscaled.mapIndexed { idx, it ->
            DefaultPoint(x=((idx+1).toDouble()/1000.0).toFloat(),(it/grandintegral).toFloat())
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

        val colors = generateHueColorPalette(legend.size)
        Row(modifier=Modifier.weight(0.5f).fillMaxSize()) {
            Column(modifier = Modifier.weight(0.5f).fillMaxSize()) {
                ChartLayout(
                    modifier = Modifier.weight(0.5f).fillMaxSize(),
                    title = {
                        Text(
                            text = "Marginal prior density of the probability parameter",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    legend = { Legend(legends = legend, colors = colors) },
                    legendLocation = LegendLocation.BOTTOM
                ) {
                    XYGraph(
                        xAxisModel = FloatLinearAxisModel(0f..1f),
                        yAxisModel = FloatLinearAxisModel(0f..maxf),
                        xAxisTitle = "Probability parameter",
                        xAxisLabels = { value -> (round(10 * value) / 10f).toString().take(3) },
                        yAxisTitle = "Density",
                    ) {
                        LinePlot(data=marginalprior,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[0]))
                        )

                        LinePlot(
                            priorw0,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush = SolidColor(colors[1]))
                        )
                        LinePlot(
                            priorw1,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[2]))
                        )
                    }
                }
                ChartLayout(
                    modifier = Modifier.weight(0.5f).fillMaxSize(),
                    title = {
                        Text(
                            text = "Marginal prior density of the weight parameter",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                ) {
                    XYGraph(
                        xAxisModel = FloatLinearAxisModel(0f..1f),
                        yAxisModel = FloatLinearAxisModel(0f..maxf),


                        xAxisTitle = "Weight",
                        xAxisLabels = { value -> (round(10 * value) / 10f).toString().take(3) },
                        yAxisTitle = "Density",

                        ) {
                        LinePlot(
                            data = wprior,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(Color.Blue))
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(0.5f).fillMaxSize()) {
                ChartLayout(
                    modifier = Modifier.weight(0.5f).fillMaxSize(),
                    title = {
                        Text(
                            text = "Marginal posterior density of the probability parameter",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    legend = { Legend(legends = legend, colors = colors) },
                    legendLocation = LegendLocation.BOTTOM
                ) {
                    XYGraph(
                        xAxisModel = FloatLinearAxisModel(0f..1f),
                        yAxisModel = FloatLinearAxisModel(0f..maxf),
                        xAxisTitle = "Probability parameter",
                        xAxisLabels = { value -> (round(10 * value) / 10f).toString().take(3) },
                        yAxisTitle = "Density",
                    ) {
                        LinePlot(
                            marginalprob,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush=SolidColor(colors[0]))
                        )
                        LinePlot(
                            probw0,
                            lineStyle = LineStyle(strokeWidth = 2.dp,brush = SolidColor(colors[1]))
                        )
                        LinePlot(
                            probw1,
                            lineStyle = LineStyle(strokeWidth = 2.dp,
                                brush=SolidColor(colors[2]))
                        )
                    }
                }
                ChartLayout(
                    modifier = Modifier.weight(0.5f).fillMaxSize(),
                    title = {
                        Text(
                            text = "Marginal posterior density of the weight parameter",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                ) {
                    XYGraph(
                        xAxisModel = FloatLinearAxisModel(0f..1f),
                        yAxisModel = FloatLinearAxisModel(0f..maxf),


                        xAxisTitle = "Weight",
                        xAxisLabels = { value -> (round(10 * value) / 10f).toString().take(3) },
                        yAxisTitle = "Density",

                        ) {
                        LinePlot(
                            data = wpost,
                            lineStyle = LineStyle(strokeWidth = 2.dp,
                                brush=SolidColor(Color.Blue))
                        )
                    }
                }
            }
        }
    }
}