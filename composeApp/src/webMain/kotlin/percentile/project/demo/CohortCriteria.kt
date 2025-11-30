package percentile.project.demo

data class CohortCriteria(
    val id: Int,
    val name: String,
    val icdCodes: List<String> = emptyList(),
    val drugCodes: List<String> = emptyList(),
    val ageSpan: IntRange
) {
    init {
        check(name.isNotEmpty()) { "Cohort name cannot be empty" }
    }
}
