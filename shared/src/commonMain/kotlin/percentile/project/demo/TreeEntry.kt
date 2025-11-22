package percentile.project.demo

import kotlinx.serialization.Serializable

@Serializable
data class TreeEntry(
    val value: String,
    val valueLabel: String="",
    val label: String,
    val parent: String,
    var children: MutableList<String>,
    var type: String) {
    /*fun print(depth: Int = 0) {
        println( "\t".repeat(n=depth) + "$id $name")
        children.forEach { it.print(depth = depth + 1) }
    }*/
}