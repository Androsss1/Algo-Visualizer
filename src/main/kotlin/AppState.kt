import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import models.AlgorithmStep
import models.Edge
import models.ToolMode
import models.Vertex
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.collections.get
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin


class AppState {
    var vertices by mutableStateOf(listOf<Vertex>())
    var edges by mutableStateOf(listOf<Edge>())
    var currentMode by mutableStateOf(ToolMode.NONE)
    var isDirected by mutableStateOf(false)

    var selectedVertexForEdge by mutableStateOf<Vertex?>(null)
    var pendingEdgeTo by mutableStateOf<Vertex?>(null)
    var showWeightDialog by mutableStateOf(false)

    var isRunning by mutableStateOf(false)
    var isAutoPlaying by mutableStateOf(false)
    var algorithmSteps by mutableStateOf(listOf<AlgorithmStep>())
    var currentStepIndex by mutableStateOf(0)

    private var nextVertexId = 0
    private val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    fun addVertex(x: Float, y: Float, customName: String? = null) {
        val name = customName ?: if (nextVertexId < 26) alphabet[nextVertexId].toString() else "V$nextVertexId"
        vertices = vertices + Vertex(nextVertexId++, name, x, y)
        updateInitialMatrix()
    }

    fun initiateEdge(from: Vertex, to: Vertex) {
        val noDuplicate = if (isDirected) {
            edges.none { it.from == from.id && it.to == to.id }
        } else {
            edges.none { (it.from == from.id && it.to == to.id) || (it.from == to.id && it.to == from.id) }
        }
        if (from.id != to.id && noDuplicate) {
            pendingEdgeTo = to
            showWeightDialog = true
        } else {
            selectedVertexForEdge = null
        }
    }

    fun vertexIndexById(id: Int): Int = vertices.indexOfFirst { it.id == id }

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

    fun distanceToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val l2 = (x1 - x2).pow(2) + (y1 - y2).pow(2)
        if (l2 == 0f) return distance(px, py, x1, y1) 

        var t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2
        t = t.coerceIn(0f, 1f)

        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)

        return distance(px, py, projX, projY)
    }

    fun removeElement(x: Float, y: Float) {
        val clickedVertex = vertices.find { distance(it.x, it.y, x, y) < 25f }
        if (clickedVertex != null) {
            edges = edges.filter { it.from != clickedVertex.id && it.to != clickedVertex.id }
            vertices = vertices.filter { it != clickedVertex }
            updateInitialMatrix()
            return
        }
        val clickedEdge = edges.find { edge ->
            val v1 = vertices.find { it.id == edge.from }
            val v2 = vertices.find { it.id == edge.to }
            if (v1 != null && v2 != null) {
                distanceToSegment(x, y, v1.x, v1.y, v2.x, v2.y) < 15f
            } else {
                false
            }
        }

        if (clickedEdge != null) {
            edges = edges.filter { it != clickedEdge }
            if (!isDirected) {
                edges = edges.filter { !(it.from == clickedEdge.to && it.to == clickedEdge.from) }
            }
            updateInitialMatrix()
        }
    }

    fun clearGraph() {
        vertices = emptyList()
        edges = emptyList()
        nextVertexId = 0
        stopAlgorithm()
    }

    fun loadFromFile(file: File) {
        try {
            val lines = file.readLines().filter { it.isNotBlank() }
            if (lines.isEmpty()) throw Exception("Файл пуст.")

            val firstLine = lines[0].trim().uppercase()
            if (firstLine != "DIRECTED" && firstLine != "UNDIRECTED") {
                throw Exception("Первая строка должна быть строго 'DIRECTED' или 'UNDIRECTED'.")
            }

            val edgeData = mutableListOf<Triple<String, String, Int>>()
            val uniqueNodes = mutableSetOf<String>()

            for (i in 1 until lines.size) {
                val parts = lines[i].trim().split(Regex("\\s+"))
                if (parts.size != 3) {
                    throw Exception("Строка ${i + 1}: должно быть ровно 3 значения (Откуда Куда Вес).")
                }

                val from = parts[0]
                val to = parts[1]
                val weight = parts[2].toIntOrNull() ?: throw Exception("Строка ${i + 1}: вес '${parts[2]}' не является целым числом.")

                uniqueNodes.add(from)
                uniqueNodes.add(to)
                edgeData.add(Triple(from, to, weight))
            }
            clearGraph()
            isDirected = firstLine == "DIRECTED"

            val centerX = 400f
            val centerY = 350f
            val radius = 200f
            val nodesList = uniqueNodes.toList().sorted()
            val angleStep = 2 * Math.PI / (nodesList.size.coerceAtLeast(1))

            nodesList.forEachIndexed { index, name ->
                val angle = index * angleStep
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                addVertex(x, y, name)
            }

            edgeData.forEach { (fromName, toName, weight) ->
                val v1 = vertices.find { it.name == fromName }
                val v2 = vertices.find { it.name == toName }
                if (v1 != null && v2 != null) {
                    val already = edges.any { e ->
                        if (isDirected) e.from == v1.id && e.to == v2.id
                        else (e.from == v1.id && e.to == v2.id) || (e.from == v2.id && e.to == v1.id)
                    }
                    if (!already) edges = edges + Edge(v1.id, v2.id, weight)
                }
            }
            updateInitialMatrix()

            if (algorithmSteps.isNotEmpty()) {
                algorithmSteps = listOf(algorithmSteps[0].copy(logMessage = "✅ Граф успешно загружен из файла. Нажмите '▶ Запуск'."))
            }

        } catch (e: Exception) {
            updateInitialMatrix()
            val baseMatrix = if (algorithmSteps.isNotEmpty()) algorithmSteps[0].matrix else Array(0) { IntArray(0) }
            algorithmSteps = listOf(AlgorithmStep(baseMatrix, "❌ Ошибка файла: ${e.message}"))
        }
    }
    fun findBridges(dist: Array<IntArray>): List<Edge> {
        val n = vertices.size
        val adj = Array(n) { mutableListOf<Edge>() }
        edges.forEach { e ->
            val u = vertexIndexById(e.from)
            val v = vertexIndexById(e.to)
            if (u != -1 && v != -1) { adj[u].add(e); adj[v].add(e) }
        }
        val visited = BooleanArray(n)
        val tin = IntArray(n) { -1 }
        val low = IntArray(n) { -1 }
        val bridges = mutableListOf<Edge>()
        var timer = 0

        fun dfs(v: Int, parent: Edge?) {
            visited[v] = true
            tin[v] = timer; low[v] = timer; timer++
            for (e in adj[v]) {
                val toId = if (e.from == vertices[v].id) e.to else e.from
                val to = vertexIndexById(toId)
                if (to == -1) continue
                if (e === parent) continue
                if (visited[to]) {
                    low[v] = minOf(low[v], tin[to])
                } else {
                    dfs(to, e)
                    low[v] = minOf(low[v], low[to])
                    if (low[to] > tin[v]) bridges.add(Edge(e.from, e.to, e.weight))
                }
            }
        }

        for (i in 0 until n) if (!visited[i]) dfs(i, null)
        return bridges
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
            val fromIdx = vertexIndexById(e.from)
            val toIdx = vertexIndexById(e.to)
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
        isAutoPlaying = false
        val steps = mutableListOf<AlgorithmStep>()
        val n = vertices.size
        val dist = Array(n) { IntArray(n) { INF } }

        for (i in 0 until n) dist[i][i] = 0
        steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), "Шаг 1: Инициализация. Главная диагональ заполнена нулями."))

        edges.forEach { e ->
            val u = vertexIndexById(e.from)
            val v = vertexIndexById(e.to)
            if (u != -1 && v != -1) {
                dist[u][v] = e.weight
                steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), "Перенос прямого пути: ${vertices[u].name} → ${vertices[v].name} = ${e.weight}", activeI = u, activeJ = v, activeK = null))
                if (!isDirected) {
                    dist[v][u] = e.weight
                    steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), "Перенос обратного пути: ${vertices[v].name} → ${vertices[u].name} = ${e.weight}", activeI = v, activeJ = u, activeK = null))
                }
            }
        }

        steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), "Шаг 2: Поиск кратчайших путей через промежуточные вершины."))

        for (k in 0 until n) {
            val phaseHeader = "--- [ ЭТАП k = ${vertices[k].name} ] ---\n"
            for (i in 0 until n) {
                for (j in 0 until n) {
                    if (i != k && j != k && i != j) {
                        val current = dist[i][j]
                        val ik = dist[i][k]
                        val kj = dist[k][j]

                        if (ik != INF && kj != INF) {
                            val newPath = ik + kj
                            val curStr = if (current == INF) "∞" else current.toString()
                            val msg = phaseHeader + "Проверка пути ${vertices[i].name}→${vertices[j].name} через ${vertices[k].name}:\nСумма: $ik + $kj = $newPath. "

                            if (newPath < current) {
                                dist[i][j] = newPath
                                steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), msg + "($newPath < $curStr) -> Обновляем!", i, j, k))
                            } else {
                                steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), msg + "($newPath >= $curStr) -> Игнорируем.", i, j, k))
                            }
                        }
                    }
                }
            }
        }

        val bridges = findBridges(dist)
        val msg = buildString {
            append("Алгоритм завершен. Все кратчайшие пути найдены.")
            if (bridges.isNotEmpty()) {
                bridges.forEach { b ->
                    val v1 = vertices.first { it.id == b.from }
                    val v2 = vertices.first { it.id == b.to }
                    append("\n[Примечание]: вершины ${v1.name} и ${v2.name} находятся в разных компонентах связности,путь между ними не существует\n")
                }
            }
        }

        steps.add(AlgorithmStep(dist.map{it.clone()}.toTypedArray(),msg))
        algorithmSteps = steps
        currentStepIndex = 0
    }




    fun stopAlgorithm() {
        isRunning = false
        isAutoPlaying = false
        updateInitialMatrix()
    }

    fun toggleAutoPlay() {
        if (currentStepIndex == algorithmSteps.size - 1) currentStepIndex = 0
        isAutoPlaying = !isAutoPlaying
    }

    fun stepForward() { if (currentStepIndex < algorithmSteps.size - 1) currentStepIndex++ }
    fun stepBackward() { if (currentStepIndex > 0) currentStepIndex-- }
}



fun openFileDialog(): File? {
    val dialog = FileDialog(null as Frame?, "Выберите файл графа (.txt)", FileDialog.LOAD)
    dialog.file = "*.txt"
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    return if (directory != null && file != null) File(directory, file) else null
}
