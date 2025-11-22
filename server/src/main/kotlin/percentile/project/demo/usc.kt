package percentile.project.demo

import java.io.File

object usc {
    //read the usc list;
    val usc = File("codes/USC List 2023.txt")
        .readLines()
        .map {
            it
                .split("£")
                .run {
                    this[0] to this[1]
                }
        }.sortedBy { it.first }

    val root = TreeEntry(value="00000", valueLabel = "USC drug coding system", label="root", type="root", children = mutableListOf(), parent = "None")
    val entries: MutableMap<String, TreeEntry> =  mutableMapOf("00000" to root)

    init {
        usc.forEach {
            val entry = TreeEntry(
                value = it.first,
                valueLabel = it.first,
                label = it.second,
                type = "USC",
                children = mutableListOf(),
                parent =
                    if (it.first.endsWith(suffix = "000")) "00000"
                    else if (it.first.endsWith(suffix = "00")) it.first.substring(range = 0..1) + "000"
                    else if (it.first.endsWith(suffix = "0")) it.first.substring(range = 0..2) + "00"
                    else it.first.substring(range = 0..3) + "0"
            )
            if (it.first.endsWith(suffix = "000")) {
                if (entries.containsKey("00000")) entries["00000"]?.children?.add(element = it.first)
                else println("parent ${it.first} does not exists")
            } else if (it.first.endsWith(suffix = "00")) {
                if (entries.containsKey(it.first.substring(range = 0..1) + "000")) entries[it.first.substring(range = 0..1) + "000"]?.children?.add(
                    element = it.first
                )
                else println("parent ${it.first} does not exists")
            } else if (it.first.endsWith(suffix = "0")) {
                if (entries.containsKey(it.first.substring(range = 0..2) + "00")) entries[it.first.substring(range = 0..2) + "00"]?.children?.add(
                    element = it.first
                )
                else println("parent ${it.first} does not exists")
            } else {
                if (entries.containsKey(it.first.substring(range = 0..3) + "0")) entries[it.first.substring(range = 0..3) + "0"]?.children?.add(
                    element = it.first
                )
                else println("parent ${it.first} does not exists")
            }
            entries[it.first] = entry
        }


        //now read drugs;
        val generics = File("codes/generic_and_product2025.txt").readLines(Charsets.UTF_8).map { it.split("£") }


        //first read the usc codes and names. if it's not already there, it should be added;
        generics.forEach {
            if (!entries.containsKey(it[0])) {
                println(it.joinToString(separator = " ", prefix = "new usc from data: "))
                val entry = TreeEntry(
                    value = it[0],
                    valueLabel = it[0],
                    label = it[1],
                    type = "USC",
                    children = mutableListOf(),
                    parent =
                        if (it[0].endsWith(suffix = "000")) "00000"
                        else if (it[0].endsWith(suffix = "00")) it[0].substring(range = 0..1) + "000"
                        else if (it[0].endsWith(suffix = "0") && entries.containsKey(it[0].substring(range = 0..2) + "00")) it[0].substring(
                            range = 0..2
                        ) + "00"
                        else if (it[0].endsWith(suffix = "0") && entries.containsKey(it[0].substring(range = 0..1) + "000")) it[0].substring(
                            range = 0..1
                        ) + "000"
                        else if (entries.containsKey(it[0].substring(range = 0..3) + "0")) it[0].substring(range = 0..3) + "0"
                        else if (entries.containsKey(it[0].substring(range = 0..2) + "00")) it[0].substring(range = 0..2) + "00"
                        else it[0].substring(range = 0..1) + "000"
                )
                //println(it.joinToString(" "))
                if (it[0].endsWith(suffix = "000")) {
                    if (entries.containsKey("00000")) entries["00000"]?.children?.add(element = it[0])
                    else println("parent ${it[0]} does not exists")
                } else if (it[0].endsWith(suffix = "00")) {
                    if (entries.containsKey(it[0].substring(range = 0..1) + "000")) entries[it[0].substring(range = 0..1) + "000"]?.children?.add(
                        element = it[0]
                    )
                    else println("parent ${it[0]} does not exists")
                } else if (it[0].endsWith(suffix = "0")) {
                    if (entries.containsKey(it[0].substring(range = 0..2) + "00")) entries[it[0].substring(range = 0..2) + "00"]?.children?.add(
                        element = it[0]
                    )
                    else if (entries.containsKey(it[0].substring(range = 0..1) + "000")) entries[it[0].substring(range = 0..1) + "000"]?.children?.add(
                        element = it[0]
                    )
                    else println("parent ${it[0]} does not exists")
                } else {
                    if (entries.containsKey(it[0].substring(range = 0..3) + "0")) entries[it[0].substring(range = 0..3) + "0"]?.children?.add(
                        element = it[0]
                    )
                    else if (entries.containsKey(it[0].substring(range = 0..2) + "00")) entries[it[0].substring(range = 0..2) + "00"]?.children?.add(
                        element = it[0]
                    )
                    else if (entries.containsKey(it[0].substring(range = 0..1) + "000")) entries[it[0].substring(range = 0..1) + "000"]?.children?.add(
                        element = it[0]
                    )
                    else println("parent ${it[0]} does not exists")
                }
                entries[it[0]] = entry
            }
            //val entry = Drugentry(code=it[0], name=it[1], children = mutableListOf())
        }

        //read generic names;
        generics.forEach {
            if (!entries.containsKey(it[0])) {
                println(it.joinToString(separator = " ", prefix = "Problem: "))
            } else {
                if (!entries.containsKey(it[0] + "£" + it[2])) {
                    val entry = TreeEntry(
                        value = it[0] + "£" + it[2],
                        valueLabel = "",
                        label = it[2],
                        type = "generic",
                        children = mutableListOf(),
                        parent = it[0]
                    )
                    entries[it[0]]?.children?.add(it[0] + "£" + it[2])
                    entries[it[0] + "£" + it[2]] = entry
                }
            }
            //val entry = Drugentry(code=it[0], name=it[1], children = mutableListOf())
        }

        //read product names;
        generics.forEach {
            if (!entries.containsKey(it[0]) || !entries.containsKey(it[0] + "£" + it[2])) {
                println(it.joinToString(separator = " ", prefix = "Problem: "))
            } else {
                if (!entries.containsKey(it[0] + "£" + it[2] + "£" + it[3])) {
                    val entry = TreeEntry(
                        value = it[0] + "£" + it[2] + "£" + it[3],
                        valueLabel = "",
                        label = it[3],
                        type = "product",
                        children = mutableListOf(),
                        parent = it[0] + "£" + it[2]
                    )
                    entries[it[0] + "£" + it[2]]?.children?.add(it[0] + "£" + it[2] + "£" + it[3])
                    entries[it[0] + "£" + it[2] + "£" + it[3]] = entry
                }
            }
            //val entry = Drugentry(code=it[0], name=it[1], children = mutableListOf())
        }


        //print(entries.keys.joinToString (" "))

        //entries["00000"]?.print()
      //  val jsontree: JsonElement = Json.encodeToJsonElement(value = entries["00000"])
      //  File("drugs/jsontree.json").writeText(text = jsontree.toString())
        //print(jsontree)
        findCycle()
    }

    fun findCycle() {
        val visited = mutableSetOf<String>()
        val recStack = mutableSetOf<String>()

        fun dfs(nodeKey: String): Boolean {
            if (nodeKey in recStack) {
                println("Cycle detected at node: $nodeKey")
                return true
            }
            if (nodeKey in visited) return false

            visited.add(nodeKey)
            recStack.add(nodeKey)

            val node = entries[nodeKey]
            if (node != null) {
                for (childKey in node.children) {
                    if (dfs(childKey)) return true
                }
            }

            recStack.remove(nodeKey)
            return false
        }

        for (key in entries.keys) {
            if (dfs(key)) {
                println("Cycle found starting from node: $key")
                return
            }
        }
        println("No cycles detected in the tree.")
    }
}