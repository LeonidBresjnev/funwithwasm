package percentile.project.demo

import java.io.File

object Icd10 {

    val entries: MutableMap<String, TreeEntry> = mutableMapOf(
        "00000" to TreeEntry(
            value = "00000",
            valueLabel = "",
            label = "ICD 10 - CM",
            type = "root",
            parent = "None",
            children = mutableListOf()
        )
    )


    fun makeparentlist(code: String): List<String> {
        if (entries.containsKey(code)) {
            val mylist = mutableListOf<String>()
            mylist.add(entries[code]!!.parent)
            mylist.addAll(makeparentlist(code = entries[code]!!.parent))
            return mylist
        } else return emptyList()
    }

    init {
        val file = File("")
        val path = file.absolutePath
        println("aboulut path: $path")


        val icd = File("codes/Icd10_allyears2.csv")
            .readLines()
            .map {
                it.split(";")
            }

        icd.forEach {
            if (!entries.containsKey(it[2])) {
                val entry =
                    TreeEntry(
                        value = it[2],
                        valueLabel = it[2],
                        label = it[3],
                        type = "Group",
                        parent = "00000",
                        children = mutableListOf()
                    )
                entries["00000"]!!.children.add(element = it[2])
                entries[it[2]] = entry
            }

            //class
            if (!entries.containsKey(it[4])) {
                val entry =
                    TreeEntry(
                        value = it[4],
                        valueLabel = it[4],
                        label = it[5],
                        type = "Class", parent = it[2], children = mutableListOf())
                try {
                    entries[it[2]]!!.children.add(element = it[4])
                } catch (e: java.lang.NullPointerException) {
                    println(e.message + it[2])
                }
                entries[it[4]] = entry
            }

            //code
            if (!entries.containsKey(it[0])) {
                if (it[0].length == 3) {

                    val entry = TreeEntry(
                        value = it[0],
                        valueLabel = it[0],
                        label = it[1],
                        type = "code${it[0].length}",
                        parent = it[4],
                        children = mutableListOf()
                    )
                    if (entries.containsKey(it[4])) {
                        entries[it[4]]!!.children.add(element = it[0])
                        entries[it[0]] = entry
                    } else {
                        println("class missing ${it[4]}")
                    }
                } else {
                    for (l in (it[0].length - 1) downTo 2) {
                        if (it[0] == "R1115") {
                            println(it[0] + " " + it[0].substring(0..<l))
                        }
                        if (entries.containsKey(it[0].substring(0..<l))) {

                            val entry = TreeEntry(
                                value = it[0],
                                valueLabel = it[0],
                                label = it[1],
                                type = "code${it[0].length}",
                                parent = it[0].substring(0..<l),
                                children = mutableListOf()
                            )
                            entries[it[0].substring(0..<l)]!!.children.add(element = it[0])
                            entries[it[0]] = entry
                            break
                        }
                    }
                }
            }
        }
    }
}