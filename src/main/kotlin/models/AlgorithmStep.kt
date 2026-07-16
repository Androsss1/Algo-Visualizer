package models


data class AlgorithmStep(
    val matrix: Array<IntArray>,
    val logMessage: String,
    val activeI: Int? = null,
    val activeJ: Int? = null,
    val activeK: Int? = null
)
enum class ToolMode { NONE, ADD_VERTEX, ADD_EDGE, MOVE, DELETE }