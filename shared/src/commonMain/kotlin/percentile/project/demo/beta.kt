package percentile.project.demo

import kotlin.math.exp
import kotlin.math.ln

/**
 * Returns a function that calculates the probability density of a Beta distribution.
 *
 * @param a The alpha shape parameter (must be positive).
 * @param b The beta shape parameter (must be positive).
 * @return A function that takes a value x in (0, 1) and returns the density value.
 */
fun betapdf(a: Double, b: Double): (Double)->Double {
    require((a>0)&&(b>0)) { "Parameters a and b must be positive." }
    val lnBeta = lnBeta(a, b)
    return { x: Double -> 
        if (x <= 0.0 || x >= 1.0) 0.0 
        else exp((a-1)*ln(x)+(b-1)*ln(1-x) - lnBeta) 
    }
}