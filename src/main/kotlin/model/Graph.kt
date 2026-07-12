package model

data class Graph(
    val nodes: List<Node> = emptyList(),
    val edges: List<Edge> = emptyList()
)
{
    fun getNodeById(id: Int): Node? = nodes.find{it.id == id}

    fun addNode(node :Node): Graph =  copy(nodes = nodes + node)

    fun removeNode(id : Int): Graph = copy(
        nodes  = nodes.filter{it.id != id},
        edges = edges.filter{it.start != id && it.end != id}
    )

    fun addEdge(edge: Edge): Graph = copy(edges = edges + edge)
    fun removeEdge(start: Int,end: Int): Graph = copy(edges = edges - Edge(start,end))


}