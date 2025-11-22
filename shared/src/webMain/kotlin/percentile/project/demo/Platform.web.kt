package percentile.project.demo

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsModule


@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("@stdlib/math-base-special-betaln")
external fun betaln(a: Double,b: Double): Double



@OptIn(ExperimentalWasmJsInterop::class)
@JsModule(import="jstat/dist/jstat.js")
external object JStat {
    object chisquare {
        fun inv(p: Double, df: Double): Double
        fun cdf(x: Double, df: Double): Double
    }

    object normal {
        fun cdf(x: Double, mean: Double, stddev: Double): Double
    }
}

actual fun lnBeta(a: Double, b: Double): Double = betaln(a,b)

actual fun invchisq_cdf(df: Double, p: Double): Double = JStat.chisquare.inv(p, df)

actual fun chisq_cdf(df: Double, x: Double): Double = JStat.chisquare.cdf(x, df)

actual fun normal_cdf(x: Double, mean: Double, stddev: Double): Double = JStat.normal.cdf(x, mean, stddev)