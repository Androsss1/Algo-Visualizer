import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.math.*

// --- Цвета ---
val DarkBackground = Color(0xFF1E1E2E)
val PanelBackground = Color(0xFF252636)

val PrimaryBlue = Color(0xFF2563EB)     // Начальная вершина (I)
val HighlightPurple = Color(0xFF8B5CF6) // Конечная вершина (J)
val HighlightOrange = Color(0xFFF59E0B) // Промежуточная вершина (K)
val HighlightGreen = Color(0xFF10B981)  // Для инструмента рисования ребер

const val INF = 9999

@Composable
fun appScrollbarStyle() = defaultScrollbarStyle().copy(
    unhoverColor = PrimaryBlue.copy(alpha = 0.4f),
    hoverColor = PrimaryBlue,
    thickness = 8.dp
)

// --- Модели Данных ---
data class Vertex(val id: Int, var name: String, var x: Float, var y: Float)
data class Edge(val from: Int, val to: Int, var weight: Int)
data class AlgorithmStep(
    val matrix: Array<IntArray>,
    val logMessage: String,
    val activeI: Int? = null,
    val activeJ: Int? = null,
    val activeK: Int? = null
)
enum class ToolMode { NONE, ADD_VERTEX, ADD_EDGE, MOVE, DELETE }

fun distance(x1: Float, y1: Float, x2: Float, y2: Float) = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))

fun openFileDialog(): File? {
    val dialog = FileDialog(null as Frame?, "Выберите файл графа (.txt)", FileDialog.LOAD)
    dialog.file = "*.txt"
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    return if (directory != null && file != null) File(directory, file) else null
}

// --- Управление Состоянием ---
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

    // --- УМНЫЙ ПАРСЕР ТЕКСТОВОГО ФАЙЛА С ПРОВЕРКАМИ ---
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

            // Если мы дошли сюда, значит файл идеальный. Можно стирать старый граф и строить новый.
            clearGraph()
            isDirected = firstLine == "DIRECTED"

            val centerX = 400f
            val centerY = 350f
            val radius = 200f
            val nodesList = uniqueNodes.toList().sorted()
            val angleStep = 2 * Math.PI / if (nodesList.isNotEmpty()) nodesList.size else 1

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
                    edges = edges + Edge(v1.id, v2.id, weight)
                }
            }
            updateInitialMatrix()

            // Пишем в логгер об успехе
            if (algorithmSteps.isNotEmpty()) {
                algorithmSteps = listOf(algorithmSteps[0].copy(logMessage = "✅ Граф успешно загружен из файла. Нажмите '▶ Запуск'."))
            }

        } catch (e: Exception) {
            // Если была ошибка формата — старый граф не удаляется, а в логгер выводится ошибка
            updateInitialMatrix()
            val baseMatrix = if (algorithmSteps.isNotEmpty()) algorithmSteps[0].matrix else Array(0) { IntArray(0) }
            algorithmSteps = listOf(AlgorithmStep(baseMatrix, "❌ Ошибка файла: ${e.message}"))
        }
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
        isAutoPlaying = false
        val steps = mutableListOf<AlgorithmStep>()
        val n = vertices.size
        val dist = Array(n) { IntArray(n) { INF } }

        for (i in 0 until n) dist[i][i] = 0
        steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), "Шаг 1: Инициализация. Главная диагональ заполнена нулями."))

        // ФАЗА 1: Прямые пути
        edges.forEach { e ->
            val u = vertices.indexOfFirst { it.id == e.from }
            val v = vertices.indexOfFirst { it.id == e.to }
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

        // ФАЗА 2: Обходные пути
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
        steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), "Алгоритм завершен. Все кратчайшие пути найдены."))
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

@Composable
fun App() {
    val state = remember { AppState() }

    MaterialTheme(colorScheme = darkColorScheme(background = DarkBackground, surface = PanelBackground)) {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
            Row(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ControlPanel(state, modifier = Modifier.weight(1f).fillMaxHeight())
                GraphPanel(state, modifier = Modifier.weight(3.5f).fillMaxHeight())
                MatrixPanel(state, modifier = Modifier.weight(1.5f).fillMaxHeight())
            }
        }

        if (state.showWeightDialog) {
            var weightInput by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }

            AlertDialog(
                onDismissRequest = { state.cancelAddEdge() },
                title = { Text("Вес ребра", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) weightInput = it },
                        singleLine = true,
                        modifier = Modifier.focusRequester(focusRequester),
                        placeholder = { Text("Введите число", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlue, unfocusedBorderColor = Color.Gray
                        )
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                },
                confirmButton = {
                    Button(onClick = { state.confirmAddEdge(weightInput.toIntOrNull() ?: 1) }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("ОК", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { state.cancelAddEdge() }) { Text("Отмена", color = Color.Gray) }
                },
                containerColor = PanelBackground
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlPanel(state: AppState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    LaunchedEffect(state.isAutoPlaying) {
        while (state.isAutoPlaying) {
            delay(1000)
            if (state.currentStepIndex < state.algorithmSteps.size - 1) {
                state.stepForward()
            } else {
                state.isAutoPlaying = false
            }
        }
    }

    Box(modifier = modifier.background(PanelBackground, RoundedCornerShape(12.dp))) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("ИНСТРУМЕНТЫ")
            ToolButton("Добавить вершину", state.currentMode == ToolMode.ADD_VERTEX, !state.isRunning) { state.currentMode = ToolMode.ADD_VERTEX }
            ToolButton("Добавить ребро", state.currentMode == ToolMode.ADD_EDGE, !state.isRunning) { state.currentMode = ToolMode.ADD_EDGE }
            ToolButton("Переместить", state.currentMode == ToolMode.MOVE, enabled = true) { state.currentMode = ToolMode.MOVE }
            ToolButton("Удалить", state.currentMode == ToolMode.DELETE, !state.isRunning) { state.currentMode = ToolMode.DELETE }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
            SectionHeader("ГРАФ")

            Row(verticalAlignment = Alignment.CenterVertically) {
                AppButton("Загрузить", modifier = Modifier.weight(1f), enabled = !state.isRunning) {
                    val file = openFileDialog()
                    if (file != null) state.loadFromFile(file)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TooltipArea(
                    tooltip = {
                        Surface(modifier = Modifier.shadow(4.dp), color = Color(0xFF2A2B3D), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "Формат файла (.txt):\n1 строка: DIRECTED или UNDIRECTED\nДалее: Откуда Куда Вес\n(Количество вершин писать не нужно!)\n\nПример:\nDIRECTED\nA B 10\nB C 5",
                                color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 12.sp
                            )
                        }
                    }
                ) {
                    Box(modifier = Modifier.size(24.dp).border(2.dp, Color.Gray, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text("i", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            AppButton("Очистить граф", enabled = !state.isRunning) { state.clearGraph() }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
            SectionHeader("АЛГОРИТМ")
            Button(
                onClick = { if (state.isRunning) state.stopAlgorithm() else state.startAlgorithm() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (state.isRunning) "⏹ Остановка" else "▶ Запуск", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("◀ Назад", modifier = Modifier.weight(1f), enabled = state.isRunning) { state.stepBackward(); state.isAutoPlaying = false }
                AppButton("Вперед ▶", modifier = Modifier.weight(1f), enabled = state.isRunning) { state.stepForward(); state.isAutoPlaying = false }
            }

            Button(
                onClick = { state.toggleAutoPlay() },
                enabled = state.isRunning,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isAutoPlaying) HighlightOrange else PrimaryBlue,
                    disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (state.isAutoPlaying) "⏸ Пауза Авто" else "▶▶ Авто", fontSize = 14.sp, color = Color.White)
            }
        }

        VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(2.dp),
            adapter = rememberScrollbarAdapter(scrollState),
            style = appScrollbarStyle()
        )
    }
}

@Composable
fun AppButton(text: String, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)
    ) { Text(text, fontSize = 14.sp, color = Color.White) }
}

@Composable
fun ToolButton(text: String, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null
    ) { Text(text, fontSize = 14.sp, color = Color.White) }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 0.dp))
}

@Composable
fun GraphPanel(state: AppState, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(4f).fillMaxWidth().background(Color.White, RoundedCornerShape(12.dp))) {
            Canvas(modifier = Modifier.fillMaxSize()
                .pointerInput(state.currentMode, state.isRunning) {
                    if (state.isRunning) return@pointerInput
                    detectTapGestures { offset ->
                        when (state.currentMode) {
                            ToolMode.ADD_VERTEX -> state.addVertex(offset.x, offset.y)
                            ToolMode.ADD_EDGE -> {
                                val clicked = state.vertices.find { distance(it.x, it.y, offset.x, offset.y) < 25f }
                                if (clicked != null) {
                                    if (state.selectedVertexForEdge == null) state.selectedVertexForEdge = clicked
                                    else state.initiateEdge(state.selectedVertexForEdge!!, clicked)
                                } else state.selectedVertexForEdge = null
                            }
                            ToolMode.DELETE -> state.removeElement(offset.x, offset.y)
                            else -> {}
                        }
                    }
                }
                .pointerInput(state.currentMode, state.isRunning) {
                    if (state.currentMode != ToolMode.MOVE) return@pointerInput
                    var draggedVertex: Vertex? = null
                    detectDragGestures(
                        onDragStart = { offset -> draggedVertex = state.vertices.find { distance(it.x, it.y, offset.x, offset.y) < 25f } },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            draggedVertex?.let { state.moveVertex(it.id, dragAmount.x, dragAmount.y) }
                        }
                    )
                }
            ) {
                val gridSize = 40.dp.toPx()
                val gridColor = Color.LightGray.copy(alpha = 0.4f)
                for (x in 0..size.width.toInt() step gridSize.toInt()) drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                for (y in 0..size.height.toInt() step gridSize.toInt()) drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)

                val step = if (state.algorithmSteps.isNotEmpty()) state.algorithmSteps[state.currentStepIndex] else null
                val actI = step?.activeI
                val actJ = step?.activeJ
                val actK = step?.activeK

                // Отрисовка Ребер
                state.edges.forEach { edge ->
                    val v1Idx = state.vertices.indexOfFirst { it.id == edge.from }
                    val v2Idx = state.vertices.indexOfFirst { it.id == edge.to }
                    val v1 = state.vertices.getOrNull(v1Idx)
                    val v2 = state.vertices.getOrNull(v2Idx)

                    if (v1 != null && v2 != null) {
                        val isActiveEdge = if (actK != null) {
                            (v1Idx == actI && v2Idx == actK) || (v1Idx == actK && v2Idx == actJ) ||
                                    (!state.isDirected && ((v1Idx == actK && v2Idx == actI) || (v1Idx == actJ && v2Idx == actK)))
                        } else if (actI != null && actJ != null) {
                            (v1Idx == actI && v2Idx == actJ) || (!state.isDirected && (v1Idx == actJ && v2Idx == actI))
                        } else false

                        val edgeColor = when {
                            isActiveEdge && actK != null -> HighlightOrange
                            isActiveEdge && actK == null -> HighlightPurple
                            actI != null -> Color.Gray.copy(alpha = 0.3f)
                            else -> Color.Gray
                        }
                        val thickness = if (isActiveEdge) 6f else 3f

                        drawLine(color = edgeColor, start = Offset(v1.x, v1.y), end = Offset(v2.x, v2.y), strokeWidth = thickness)

                        if (state.isDirected) {
                            val angle = atan2(v2.y - v1.y, v2.x - v1.x)
                            val tipX = v2.x - 22f * cos(angle)
                            val tipY = v2.y - 22f * sin(angle)
                            val arrowLen = 15f
                            val p1X = tipX - arrowLen * cos(angle - PI / 6)
                            val p1Y = tipY - arrowLen * sin(angle - PI / 6)
                            val p2X = tipX - arrowLen * cos(angle + PI / 6)
                            val p2Y = tipY - arrowLen * sin(angle + PI / 6)

                            val arrowPath = Path().apply { moveTo(tipX, tipY); lineTo(p1X.toFloat(), p1Y.toFloat()); lineTo(p2X.toFloat(), p2Y.toFloat()); close() }
                            drawPath(arrowPath, edgeColor)
                        }

                        val midX = (v1.x + v2.x) / 2
                        val midY = (v1.y + v2.y) / 2 - 20f
                        drawText(
                            textMeasurer = textMeasurer, text = edge.weight.toString(), topLeft = Offset(midX, midY),
                            style = TextStyle(color = if (isActiveEdge) edgeColor else Color.Red.copy(alpha = if (actI != null) 0.5f else 1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Отрисовка Вершин
                state.vertices.forEachIndexed { index, vertex ->
                    val vColor = when {
                        state.selectedVertexForEdge == vertex -> HighlightGreen
                        index == actI -> PrimaryBlue
                        index == actJ -> HighlightPurple
                        actK != null && index == actK -> HighlightOrange
                        actI != null -> PrimaryBlue.copy(alpha = 0.3f)
                        else -> PrimaryBlue
                    }
                    drawCircle(color = vColor, radius = 22f, center = Offset(vertex.x, vertex.y))
                    drawText(
                        textMeasurer = textMeasurer, text = vertex.name, topLeft = Offset(vertex.x - 8f, vertex.y - 14f),
                        style = TextStyle(color = Color.White.copy(alpha = if (actI != null && vColor == PrimaryBlue.copy(alpha = 0.3f)) 0.5f else 1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF161621), RoundedCornerShape(12.dp))) {
            val logScrollState = rememberScrollState()
            Column(modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(logScrollState)) {
                Text("Лог выполнения:", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val msg = if (state.algorithmSteps.isNotEmpty()) state.algorithmSteps[state.currentStepIndex].logMessage else "Нарисуйте или загрузите граф..."
                Text(msg, color = if (msg.contains("❌")) Color.Red else Color.White, fontSize = 16.sp)
            }
            VerticalScrollbar(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(2.dp), adapter = rememberScrollbarAdapter(logScrollState), style = appScrollbarStyle())
        }
    }
}

@Composable
fun MatrixPanel(state: AppState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(PanelBackground, RoundedCornerShape(12.dp)).padding(16.dp)) {
        SectionHeader("МАТРИЦА РАССТОЯНИЙ")
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF161621), RoundedCornerShape(8.dp)).padding(8.dp)) {
            if (state.vertices.isEmpty()) {
                Text("Граф пуст", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else if (state.algorithmSteps.isNotEmpty()) {
                val step = state.algorithmSteps[state.currentStepIndex]
                val matrix = step.matrix
                val n = state.vertices.size
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(columns = GridCells.Fixed(n + 1), state = gridState) {
                    item { MatrixCell("", isHeader = true, bgColor = PanelBackground) }

                    // Верхние заголовки (Столбцы = j или k)
                    items(n) { colIndex ->
                        val hBg = when (colIndex) {
                            step.activeI -> PrimaryBlue
                            step.activeJ -> HighlightPurple
                            step.activeK -> HighlightOrange
                            else -> PanelBackground
                        }
                        MatrixCell(state.vertices[colIndex].name, isHeader = true, bgColor = hBg)
                    }

                    for (i in 0 until n) {
                        // Левые заголовки (Строки = i или k)
                        val hBg = when (i) {
                            step.activeI -> PrimaryBlue
                            step.activeJ -> HighlightPurple
                            step.activeK -> HighlightOrange
                            else -> PanelBackground
                        }
                        item { MatrixCell(state.vertices[i].name, isHeader = true, bgColor = hBg) }

                        // Ячейки
                        for (j in 0 until n) {
                            val v = matrix[i][j]
                            val isTarget = (i == step.activeI && j == step.activeJ)
                            val isComponent = if (step.activeK != null) {
                                (i == step.activeI && j == step.activeK) || (i == step.activeK && j == step.activeJ)
                            } else false

                            val bgColor = if (isComponent) HighlightOrange.copy(alpha = 0.5f) else Color.Transparent
                            val borderColor = if (isTarget) HighlightPurple else Color.DarkGray
                            val borderThickness = if (isTarget) 3.dp else 1.dp

                            item { MatrixCell(if (v == INF) "∞" else v.toString(), isHeader = false, bgColor = bgColor, borderColor = borderColor, borderWidth = borderThickness) }
                        }
                    }
                }
                VerticalScrollbar(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(2.dp), adapter = rememberScrollbarAdapter(gridState), style = appScrollbarStyle())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Color(0xFF161621), RoundedCornerShape(8.dp)).padding(12.dp)) {
            Switch(
                checked = state.isDirected, onCheckedChange = { state.toggleDirected() }, enabled = !state.isRunning,
                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue, checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = if (state.isDirected) "Ориентированный" else "Неориентированный", color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun MatrixCell(text: String, isHeader: Boolean, bgColor: Color, borderColor: Color = Color.DarkGray, borderWidth: Dp = 1.dp) {
    Box(
        modifier = Modifier.aspectRatio(1f).border(borderWidth, borderColor).background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isHeader && bgColor == PanelBackground) Color.Gray else Color.White, fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal)
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Floyd-Warshall Visualizer", state = rememberWindowState(width = 1100.dp, height = 700.dp)) {
        App()
    }
}
