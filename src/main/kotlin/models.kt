import kotlin.math.pow
import kotlin.math.sqrt

data class Vertex(val id: Int, var name: String, var x: Float, var y: Float)
data class Edge(val from: Int, val to: Int, var weight: Int)
data class AlgorithmStep(val matrix: Array<IntArray>, val logMessage: String)

enum class ToolMode { NONE, ADD_VERTEX, ADD_EDGE, MOVE, DELETE }

fun distance(x1: Float, y1: Float, x2: Float, y2: Float) = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))