package percentile.project.demo


import org.apache.commons.math3.special.Beta
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.distribution.ChiSquaredDistribution

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

/**
 * JVM implementation of platform-specific statistical functions using Apache Commons Math.
 */

actual fun lnBeta(a: Double, b: Double): Double = Beta.logBeta(a, b)

actual fun invchisq_cdf(df: Double, p: Double): Double = ChiSquaredDistribution(df).inverseCumulativeProbability(p)
actual fun chisq_cdf(df: Double, x: Double): Double = ChiSquaredDistribution(df).cumulativeProbability(x)
actual fun normal_cdf(x: Double, mean: Double, stddev: Double): Double = NormalDistribution(mean, stddev).cumulativeProbability(x)