package percentile.project.demo

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.math.abs

class BorrowingModelsTest {

    private fun integrate(f: (Double) -> Double): Double {
        val steps = 1000
        val h = 1.0 / steps
        var sum = 0.0
        for (i in 0..steps) {
            val x = if (i == 0) 1e-9 else if (i == steps) 1.0 - 1e-9 else i.toDouble() * h
            val y = f(x)
            if (i == 0 || i == steps) sum += 0.5 * y else sum += y
        }
        return sum * h
    }

    private val tolerance = 0.05 // Allowing for some numerical error as requested

    @Test
    fun testRobustMixturePriorIntegratesToOne() {
        val prior = BorrowingModels.robustMixturePrior(
            ns = 100, ps = 0.5f,
            alphaprior = 1f, betaprior = 1f,
            priorw = 0.5f
        )
        val area = integrate(prior)
        assertTrue(abs(area - 1.0) < tolerance, "Robust Mixture Prior area was $area")
    }

    @Test
    fun testRobustMixturePosteriorIntegratesToOne() {
        val post = BorrowingModels.robustMixturePosterior(
            ns = 100, ps = 0.5f,
            nt = 100, pt = 0.6f,
            alphaprior = 1f, betaprior = 1f,
            priorw = 0.5f
        )
        val area = integrate(post)
        assertTrue(abs(area - 1.0) < tolerance, "Robust Mixture Posterior area was $area")
    }

    @Test
    fun testNppPriorIntegratesToOne() {
        val prior = BorrowingModels.nppPrior(
            ns = 10, ps = 0.5f,
            alphaprior = 1f, betaprior = 1f,
            alphaw = 1f, betaw = 1f
        )
        val area = integrate(prior)
        assertTrue(abs(area - 1.0) < tolerance, "NPP Prior area was $area")
    }

    @Test
    fun testNppPosteriorIntegratesToOne() {
        val post = BorrowingModels.nppPosterior(
            ns = 10, ps = 0.5f,
            nt = 10, pt = 0.6f,
            alphaprior = 1f, betaprior = 1f,
            alphaw = 1f, betaw = 1f
        )
        val area = integrate(post)
        assertTrue(abs(area - 1.0) < tolerance, "NPP Posterior area was $area")
    }

    @Test
    fun testPValueBasedPriorIntegratesToOne() {
        val prior = BorrowingModels.pValueBasedPrior(
            ns = 100, ps = 0.5f,
            nt = 100, pt = 0.5f,
            alphaprior = 1f, betaprior = 1f,
            kappa = 1f, lambda = 1f
        )
        val area = integrate(prior)
        println("[DEBUG_LOG] P-Value Based Prior area: $area")
        assertTrue(abs(area - 1.0) < tolerance, "P-Value Based Prior area was $area")
    }

    @Test
    fun testPValueBasedPosteriorIntegratesToOne() {
        val post = BorrowingModels.pValueBasedPosterior(
            ns = 100, ps = 0.5f,
            nt = 100, pt = 0.6f,
            alphaprior = 1f, betaprior = 1f,
            kappa = 1f, lambda = 1f
        )
        val area = integrate(post)
        println("[DEBUG_LOG] P-Value Based Posterior area: $area")
        assertTrue(abs(area - 1.0) < tolerance, "P-Value Based Posterior area was $area")
    }
}
