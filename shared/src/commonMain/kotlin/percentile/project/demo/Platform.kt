package percentile.project.demo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun lnBeta(a: Double, b: Double): Double

expect fun invchisq_cdf(df: Double, p: Double): Double

expect fun chisq_cdf(df: Double, x: Double): Double

expect fun normal_cdf(x: Double, mean: Double, stddev: Double): Double