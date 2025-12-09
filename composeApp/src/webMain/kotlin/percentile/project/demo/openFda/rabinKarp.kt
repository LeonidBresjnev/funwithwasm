package percentile.project.demo.openFda

const val x=31L
const val p= 101

fun polyhash(word: String) : Long {

    var hash = 0L

    for (i in word.indices) {
        hash = (hash * x) % p+ word.elementAt(i).code % p
    }

    return hash % p
}

fun String.rabinKarp(word: String): Collection<Int> {
    val unquotedWord = word.removeSurrounding("\"")
    val m = unquotedWord.length
    if (this.length < m) return emptyList()
    val hash0 = polyhash(word = unquotedWord)

    val result = mutableListOf<Int>()
    var hash1: Long = polyhash(word=this.substring(0,m))

    var xm = 1L
    for (i in 1 until m) {
        xm = (xm * x) % p
    }
    if (hash1 == hash0 && this.substring(0, m) == unquotedWord) {
        result.add(0)
    }

    for (i in 1..this.length-m) {
        hash1 = (p + x* (hash1  - this.elementAt(i-1).code * xm) % p + this.elementAt(i+m-1).code % p) % p
        if (hash1 == hash0 && this.substring(i, i+m) == unquotedWord) {
            result.add(i)
        }
    }
    return result
}