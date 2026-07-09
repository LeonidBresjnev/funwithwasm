package percentile.project.demo

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.absoluteValue
import kotlin.math.sqrt

/**
 * Object containing implementations of various statistical borrowing models.
 * Borrowing models are used to incorporate information from source data (historical)
 * into the analysis of target data (current study).
 */
object BorrowingModels {

    /**
     * Calculates the Robust Mixture Prior distribution.
     * This model uses a mixture of an informative prior (based on source data)
     * and a non-informative (robust) prior.
     *
     * @param ns Number of subjects in the source data.
     * @param ps Proportion of successes in the source data.
     * @param alphaprior Alpha parameter for the base Beta prior.
     * @param betaprior Beta parameter for the base Beta prior.
     * @param priorw Prior weight given to the informative component.
     * @return A function that takes a probability p and returns the density value.
     */
    fun robustMixturePrior(
        ns: Int, ps: Float,
        alphaprior: Float, betaprior: Float,
        priorw: Float
    ): (Double) -> Double {
        val xs = ps * ns
        // m1 is the informative component (Beta distribution updated with source data)
        val m1 = betapdf(xs + alphaprior.toDouble(), (ns - xs) + betaprior.toDouble())
        // m0 is the robust (non-informative) component
        val m0 = betapdf(alphaprior.toDouble(), betaprior.toDouble())
        return { p -> priorw * m1(p) + (1 - priorw) * m0(p) }
    }

    /**
     * Calculates the Robust Mixture Posterior distribution.
     *
     * @param ns Number of subjects in the source data.
     * @param ps Proportion of successes in the source data.
     * @param nt Number of subjects in the target data.
     * @param pt Proportion of successes in the target data.
     * @param alphaprior Alpha parameter for the base Beta prior.
     * @param betaprior Beta parameter for the base Beta prior.
     * @param priorw Prior weight given to the informative component.
     * @return A function that takes a probability p and returns the density value.
     */
    fun robustMixturePosterior(
        ns: Int, ps: Float,
        nt: Int, pt: Float,
        alphaprior: Float, betaprior: Float,
        priorw: Float
    ): (Double) -> Double {
        val xs = ps * ns
        val xt = pt * nt

        // Calculate the posterior weight for the informative component
        val postwUnweighted1 = priorw * exp(
            lnBeta((xt + xs + alphaprior).toDouble(), ((nt - xt) + (ns - xs) + betaprior).toDouble())
                    - lnBeta((xs + alphaprior).toDouble(), ((ns - xs) + betaprior).toDouble())
        )
        val postwUnweighted2 = (1.0 - priorw) * exp(
            lnBeta((xt + alphaprior).toDouble(), (nt - xt + betaprior).toDouble())
                    - lnBeta(alphaprior.toDouble(), betaprior.toDouble())
        )
        val postw = (postwUnweighted1 / (postwUnweighted1 + postwUnweighted2))

        // m1post is the informative component posterior
        val m1post = betapdf((xt + xs + alphaprior).toDouble(), (nt - xt + (ns - xs) + betaprior).toDouble())
        // m0post is the robust component posterior
        val m0post = betapdf((xt + alphaprior).toDouble(), (nt - xt + betaprior).toDouble())

        return { p -> postw * m1post(p) + (1 - postw) * m0post(p) }
    }

    /**
     * Calculates the Normalized Power Prior (NPP) distribution.
     * NPP discounts the source data by a power factor w, which is treated as a random variable.
     *
     * @param ns Number of subjects in the source data.
     * @param ps Proportion of successes in the source data.
     * @param alphaprior Alpha parameter for the base Beta prior.
     * @param betaprior Beta parameter for the base Beta prior.
     * @param alphaw Alpha parameter for the Beta prior on the weight w.
     * @param betaw Beta parameter for the Beta prior on the weight w.
     * @return A function that takes a probability p and returns the density value.
     */
    fun nppPrior(
        ns: Int, ps: Float,
        alphaprior: Float, betaprior: Float,
        alphaw: Float, betaw: Float
    ): (Double) -> Double {
        val xs = ps * ns
        val wbetacoeff = lnBeta(alphaw.toDouble(), betaw.toDouble())
        
        return { p ->
            // Marginalize over w in [0, 1] using numerical integration (trapezoidal rule)
            val steps = 1000
            val h = 1.0 / steps
            var sum = 0.0
            for (i in 0..steps) {
                val w = if (i == 0) 1e-9 else if (i == steps) 1.0 - 1e-9 else i.toDouble() * h
                val c = lnBeta(w * xs + alphaprior, w * (ns - xs) + betaprior)
                // Density of p given w
                val pdf_p_given_w = exp(
                    ln(p) * (w * xs + alphaprior - 1.0) + ln(1.0 - p) * (w * (ns - xs) + betaprior - 1.0) - c
                )
                // Density of w
                val pdf_w = exp((alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w) - wbetacoeff)
                val term = pdf_p_given_w * pdf_w
                if (i == 0 || i == steps) sum += 0.5 * term else sum += term
            }
            sum * h
        }
    }

    /**
     * Calculates the Normalized Power Prior Posterior distribution.
     */
    fun nppPosterior(
        ns: Int, ps: Float,
        nt: Int, pt: Float,
        alphaprior: Float, betaprior: Float,
        alphaw: Float, betaw: Float
    ): (Double) -> Double {
        val xs = ps * ns
        val xt = pt * nt
        val wbetacoeff = lnBeta(alphaw.toDouble(), betaw.toDouble())

        // First calculate the normalizing constant (marginal likelihood) via numerical integration
        val steps = 1000
        val h = 1.0 / steps
        var marginalLikelihood = 0.0
        for (i in 0..steps) {
            val w = if (i == 0) 1e-9 else if (i == steps) 1.0 - 1e-9 else i.toDouble() * h
            val term = exp(
                lnBeta((xt + w * xs + alphaprior), nt - xt + w * (ns - xs) + betaprior) -
                        lnBeta(w * xs + alphaprior, w * (ns - xs) + betaprior) +
                        (alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w) - wbetacoeff
            )
            if (i == 0 || i == steps) marginalLikelihood += 0.5 * term else marginalLikelihood += term
        }
        marginalLikelihood *= h

        return { p ->
            // Marginalize over w for the joint density and divide by marginal likelihood
            var sum = 0.0
            for (i in 0..steps) {
                val w = if (i == 0) 1e-9 else if (i == steps) 1.0 - 1e-9 else i.toDouble() * h
                val c = lnBeta(w * xs + alphaprior, w * (ns - xs) + betaprior)
                val joint = exp(
                    ln(p) * (xt + w * xs + alphaprior - 1.0) + ln(1.0 - p) * ((nt - xt) + w * (ns - xs) + betaprior - 1.0) - c +
                            (alphaw - 1.0) * ln(w) + (betaw - 1.0) * ln(1.0 - w) - wbetacoeff
                )
                if (i == 0 || i == steps) sum += 0.5 * joint else sum += joint
            }
            (sum * h) / marginalLikelihood
        }
    }

    /**
     * Calculates the weight for P-Value Based Borrowing.
     * The weight depends on the similarity between source and target data as measured by a p-value.
     */
    fun pValueBasedWeight(
        ns: Int, ps: Float,
        nt: Int, pt: Float,
        alphaprior: Float, betaprior: Float,
        kappa: Float, lambda: Float
    ): Double {
        val xs = ps * ns
        val xt = pt * nt

        // Normal approximation for the difference in log-odds
        val p1 = ((xs + alphaprior) / (ns + alphaprior + betaprior)).toDouble()
        val theta1 = ln(p1 / (1 - p1))
        val v1 = 1 / ((ns + betaprior + alphaprior) * p1 * (1 - p1))

        val p2 = ((xt + alphaprior) / (nt + alphaprior + betaprior)).toDouble()
        val theta2 = ln(p2 / (1 - p2))
        val v2 = 1 / ((nt + betaprior + alphaprior) * p2 * (1 - p2))

        val mean = theta1 - theta2
        val sd = sqrt(v1 + v2)

        val threshold = ln(lambda.toDouble()).absoluteValue
        val pval = 1.0 - normal_cdf(threshold, mean, sd) + normal_cdf(-threshold, mean, sd)
        return exp(kappa * ln(1 - pval) / (1 - pval))
    }

    /**
     * Calculates the P-Value Based Prior distribution.
     */
    fun pValueBasedPrior(
        ns: Int, ps: Float,
        nt: Int, pt: Float,
        alphaprior: Float, betaprior: Float,
        kappa: Float, lambda: Float
    ): (Double) -> Double {
        val weight = pValueBasedWeight(ns, ps, nt, pt, alphaprior, betaprior, kappa, lambda)
        val xs = ps * ns
        // Prior is a Beta distribution where the source data sample size is discounted by 'weight'
        return betapdf(weight * xs + alphaprior, weight * (ns - xs) + betaprior)
    }

    /**
     * Calculates the P-Value Based Posterior distribution.
     */
    fun pValueBasedPosterior(
        ns: Int, ps: Float,
        nt: Int, pt: Float,
        alphaprior: Float, betaprior: Float,
        kappa: Float, lambda: Float
    ): (Double) -> Double {
        val weight = pValueBasedWeight(ns, ps, nt, pt, alphaprior, betaprior, kappa, lambda)
        val xs = ps * ns
        val xt = pt * nt
        // Posterior updates the weighted prior with target data
        return betapdf(xt + weight * xs + alphaprior, (nt - xt) + weight * (ns - xs) + betaprior)
    }
}
