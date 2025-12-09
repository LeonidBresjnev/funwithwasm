package percentile.project.demo

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIInput(
    val role: String,
    val content: String
)
