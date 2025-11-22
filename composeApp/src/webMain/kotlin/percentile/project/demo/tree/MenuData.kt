package percentile.project.demo.tree

interface MenuData {
    val id: Int
    val parentId: Int?
    val displayName: String
    var children: Collection<MenuData>
}