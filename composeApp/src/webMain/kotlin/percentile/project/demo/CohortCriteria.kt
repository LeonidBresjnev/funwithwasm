package percentile.project.demo

data class CohortCriteria(
    val id: Int,
    val icdCodes: List<String> = emptyList(),
    val drugCodes: List<String> = emptyList()
)
