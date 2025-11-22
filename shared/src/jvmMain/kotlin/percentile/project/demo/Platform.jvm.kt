package percentile.project.demo


import org.apache.commons.math3.special.Beta

class JVMPlatform : Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun lnBeta(a: Double, b: Double): Double = Beta.logBeta(a, b)

actual fun invchisq_cdf(df: Double, p: Double): Double =0.0
actual fun chisq_cdf(df: Double, x: Double): Double = 0.0
actual fun normal_cdf(x: Double, mean: Double, stddev: Double): Double = 0.0