import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class AppState {
    var vertices by mutableStateOf(listOf<Vertex>())
    var edges by mutableStateOf(listOf<Edge>())
    var currentMode by mutableStateOf(ToolMode.NONE)

    var isDirected by mutableStateOf(false)

    var selectedVertexForEdge by mutableStateOf<Vertex?>(null)
    var pendingEdgeTo by mutableStateOf<Vertex?>(null)
    var showWeightDialog by mutableStateOf(false)

    var isRunning by mutableStateOf(false)
    var algorithmSteps by mutableStateOf(listOf<AlgorithmStep>())
    var currentStepIndex by mutableStateOf(0)

    private var nextVertexId = 0
    private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun addVertex(x: Float, y: Float) {
        val name = if (nextVertexId < 26) alphabet[nextVertexId].toString() else "V$nextVertexId"
        vertices = vertices + Vertex(nextVertexId++, name, x, y)
        updateInitialMatrix()
    }

    fun initiateEdge(from: Vertex, to: Vertex) {
        if (from.id != to.id && edges.none { it.from == from.id && it.to == to.id }) {
            pendingEdgeTo = to
            showWeightDialog = true
        } else {
            selectedVertexForEdge = null
        }
    }

    fun confirmAddEdge(weight: Int) {
        if (selectedVertexForEdge != null && pendingEdgeTo != null) {
            edges = edges + Edge(selectedVertexForEdge!!.id, pendingEdgeTo!!.id, weight)
            updateInitialMatrix()
        }
        cancelAddEdge()
    }

    fun cancelAddEdge() {
        showWeightDialog = false
        selectedVertexForEdge = null
        pendingEdgeTo = null
    }

    fun moveVertex(id: Int, dx: Float, dy: Float) {
        vertices = vertices.map { if (it.id == id) it.copy(x = it.x + dx, y = it.y + dy) else it }
    }

    fun removeElement(x: Float, y: Float) {
        val clicked = vertices.find { distance(it.x, it.y, x, y) < 25f }
        if (clicked != null) {
            edges = edges.filter { it.from != clicked.id && it.to != clicked.id }
            vertices = vertices.filter { it != clicked }
            updateInitialMatrix()
        }
    }

    fun clearGraph() {
        vertices = emptyList()
        edges = emptyList()
        nextVertexId = 0
        stopAlgorithm()
    }

    fun loadExample() {
        clearGraph()
        addVertex(100f, 100f); addVertex(300f, 50f); addVertex(400f, 200f); addVertex(200f, 300f)
        edges = listOf(Edge(0, 1, 3), Edge(1, 2, 2), Edge(2, 3, 1), Edge(0, 3, 7), Edge(3, 1, 6))
        updateInitialMatrix()
    }

    fun toggleDirected() {
        if (!isRunning) {
            isDirected = !isDirected
            updateInitialMatrix()
        }
    }

    private fun updateInitialMatrix() {
        if (isRunning) return
        val n = vertices.size
        val matrix = Array(n) { IntArray(n) { INF } }
        for (i in 0 until n) matrix[i][i] = 0
        edges.forEach { e ->
            val fromIdx = vertices.indexOfFirst { it.id == e.from }
            val toIdx = vertices.indexOfFirst { it.id == e.to }
            if (fromIdx != -1 && toIdx != -1) {
                matrix[fromIdx][toIdx] = e.weight
                if (!isDirected) matrix[toIdx][fromIdx] = e.weight
            }
        }
        algorithmSteps = listOf(AlgorithmStep(matrix, "Граф готов. Нажмите 'Запуск'."))
        currentStepIndex = 0
    }

    fun startAlgorithm() {
        if (vertices.isEmpty()) return
        isRunning = true
        val steps = mutableListOf<AlgorithmStep>()
        val n = vertices.size
        val dist = Array(n) { IntArray(n) { INF } }

        for (i in 0 until n) dist[i][i] = 0
        edges.forEach { e ->
            val fromIdx = vertices.indexOfFirst { it.id == e.from }
            val toIdx = vertices.indexOfFirst { it.id == e.to }
            dist[fromIdx][toIdx] = e.weight
            if (!isDirected) dist[toIdx][fromIdx] = e.weight
        }

        steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), "Инициализация: базовые пути добавлены в матрицу."))

        for (k in 0 until n) {
            for (i in 0 until n) {
                for (j in 0 until n) {
                    if (i != k && j != k && i != j) {
                        val current = dist[i][j]
                        val ik = dist[i][k]
                        val kj = dist[k][j]

                        if (ik != INF && kj != INF) {
                            val newPath = ik + kj
                            val curStr = if (current == INF) "∞" else current.toString()
                            val msg = "Путь ${vertices[i].name}→${vertices[j].name} через ${vertices[k].name}: $ik + $kj = $newPath. "

                            if (newPath < current) {
                                dist[i][j] = newPath
                                steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), msg + "($newPath < $curStr) Обновляем!"))
                            } else {
                                steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), msg + "($newPath >= $curStr) Игнорируем."))
                            }
                        }
                    }
                }
            }
        }
        steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), "Алгоритм завершен. Все кратчайшие пути найдены."))
        algorithmSteps = steps
        currentStepIndex = 0
    }

    fun stopAlgorithm() {
        isRunning = false
        updateInitialMatrix()
    }

    fun stepForward() { if (currentStepIndex < algorithmSteps.size - 1) currentStepIndex++ }
    fun stepBackward() { if (currentStepIndex > 0) currentStepIndex-- }
}