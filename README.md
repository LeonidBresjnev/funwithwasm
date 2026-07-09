# FunWithWasm - Borrowing Information Models

[![Deploy Frontend to GitHub Pages](https://github.com/percentile-project/funwithwasm/actions/workflows/deploy.yml/badge.svg)](https://github.com/percentile-project/funwithwasm/actions/workflows/deploy.yml)

This project demonstrates several statistical methods for borrowing information from historical data in clinical trials or other research settings, implemented using Kotlin Wasm and Compose HTML.

## Borrowing Information Methods

The application implements and visualizes the following methods for information borrowing:

### 1. Robust Mixture Priors
This method uses a mixture of an informative prior (based on historical data) and a non-informative "robust" component. The mixture approach allows for borrowing information when the historical and current data are consistent, while the robust component protects against "prior-data conflict" when the data sources disagree.

### 2. Normalized Power Priors
Normalized Power Priors (NPP) provide a formal Bayesian framework for borrowing information by discounting the historical likelihood. The discounting is controlled by a power parameter (weight), which is itself treated as a random variable. The "normalized" aspect ensures that the resulting prior is a proper probability distribution, allowing for valid Bayesian inference and weight estimation.

### 3. P-Value Based Borrowing
This approach uses a frequentist-inspired mechanism to determine the amount of information to borrow. It calculates a measure of similarity (or "p-value") between the historical data and the current data. The weight assigned to the historical information is then a function of this similarity measure, typically using parameters like Kappa and Lambda to control the aggressiveness of the borrowing.
