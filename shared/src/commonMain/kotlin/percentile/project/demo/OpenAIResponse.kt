package percentile.project.demo

import kotlinx.serialization.Serializable

@Serializable
class OpenAIResponse(
    val id: String,
    val status: String,
    val output: List<OpenAIOutput>
)

@Serializable
data class OpenAIOutput(
    val id: String,
    val type: String,
    val content: List<OpenAIContent> = emptyList()
)

@Serializable
data class OpenAIContent(
   val type: String="",
    val text: String=""
)