package percentile.project.demo

import kotlin.math.exp
import kotlin.math.ln

fun betapdf(a: Double, b: Double): (Double)->Double {
    require((a>0)&&(b>0)) { "Parameters a and b must be positive." }
    val lnBeta = lnBeta(a, b)
    return { x: Double -> exp((a-1)*ln(x)+(b-1)*ln(1-x) - lnBeta) }
}