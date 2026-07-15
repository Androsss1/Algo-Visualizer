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
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import kotlin.math.*

// --- Цвета ---
val DarkBackground = Color(0xFF1E1E2E)
val PanelBackground = Color(0xFF252636)
val PrimaryBlue = Color(0xFF2563EB)
const val INF = 9999

// Единый стиль для всех ползунков прокрутки (делаем их заметными)
@Composable
fun appScrollbarStyle() = defaultScrollbarStyle().copy(
    unhoverColor = PrimaryBlue.copy(alpha = 0.4f),
    hoverColor = PrimaryBlue,
    thickness = 8.dp
)

// --- Модели Данных ---
data class Vertex(val id: Int, var name: String, var x: Float, var y: Float)
data class Edge(val from: Int, val to: Int, var weight: Int)
data class AlgorithmStep(val matrix: Array<IntArray>, val logMessage: String)
enum class ToolMode { NONE, ADD_VERTEX, ADD_EDGE, MOVE, DELETE }

fun distance(x1: Float, y1: Float, x2: Float, y2: Float) = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))

// --- Вызов системного окна для выбора файла ---
fun openFileDialog(): File? {
    val dialog = FileDialog(null as Frame?, "Выберите файл графа (.txt)", FileDialog.LOAD)
    dialog.file = "*.txt"
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    return if (directory != null && file != null) File(directory, file) else null
}

// --- Управление Состоянием (V1) ---
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


    // Расстояние от точки (px, py) до отрезка (x1, y1) - (x2, y2)
    fun distanceToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val l2 = (x1 - x2).pow(2) + (y1 - y2).pow(2)
        if (l2 == 0f) return distance(px, py, x1, y1) // Отрезок вырожден в точку

        // Проекция точки на прямую, содержащую отрезок
        var t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2
        t = t.coerceIn(0f, 1f) // Ограничиваем проекцию пределами отрезка

        val projX = x1 + t * (x2 - x1)
        val projY = y1 + t * (y2 - y1)

        return distance(px, py, projX, projY)
    }

    fun removeElement(x: Float, y: Float) {
        // 1. Сначала проверяем, не кликнули ли мы по вершине (радиус 25f, как при добавлении)
        val clickedVertex = vertices.find { distance(it.x, it.y, x, y) < 25f }
        if (clickedVertex != null) {
            // Если кликнули по вершине, удаляем её и все связанные с ней ребра
            edges = edges.filter { it.from != clickedVertex.id && it.to != clickedVertex.id }
            vertices = vertices.filter { it != clickedVertex }
            updateInitialMatrix()
            return
        }

        // 2. Если не по вершине, проверяем, не кликнули ли мы по ребру (допуск 15 пикселей от линии)
        val clickedEdge = edges.find { edge ->
            val v1 = vertices.find { it.id == edge.from }
            val v2 = vertices.find { it.id == edge.to }
            if (v1 != null && v2 != null) {
                distanceToSegment(x, y, v1.x, v1.y, v2.x, v2.y) < 15f
            } else {
                false
            }
        }

        // 3. Если ребро найдено, удаляем только его
        if (clickedEdge != null) {
            edges = edges.filter { it != clickedEdge }
            updateInitialMatrix()
        }
    }




    fun clearGraph() {
        vertices = emptyList()
        edges = emptyList()
        nextVertexId = 0
        stopAlgorithm()
    }

    // --- ПАРСЕР ТЕКСТОВОГО ФАЙЛА ---
    fun loadFromFile(file: File) {
        try {
            val lines = file.readLines().filter { it.isNotBlank() }
            if (lines.isEmpty()) return

            clearGraph()

            val firstLine = lines[0].trim().uppercase()
            isDirected = firstLine == "DIRECTED"

            val edgeData = mutableListOf<Triple<String, String, Int>>()
            val uniqueNodes = mutableSetOf<String>()

            for (i in 1 until lines.size) {
                val parts = lines[i].trim().split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val from = parts[0]
                    val to = parts[1]
                    val weight = if (parts.size >= 3) parts[2].toIntOrNull() ?: 1 else 1

                    uniqueNodes.add(from)
                    uniqueNodes.add(to)
                    edgeData.add(Triple(from, to, weight))
                }
            }

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

        } catch (e: Exception) {
            algorithmSteps = listOf(AlgorithmStep(Array(0) { IntArray(0) }, "Ошибка загрузки: Файл имеет неверный формат."))
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
                                steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), msg + "($newPath < $curStr) -> Обновляем!"))
                            } else {
                                steps.add(AlgorithmStep(dist.map { it.clone() }.toTypedArray(), msg + "($newPath >= $curStr) -> Игнорируем."))
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

        // Диалог ввода веса с АВТОФОКУСОМ
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

                    // Запрос фокуса при открытии диалога
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
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

// ДОБАВЛЕНА АННОТАЦИЯ ДЛЯ РАЗРЕШЕНИЯ ИСПОЛЬЗОВАНИЯ TOOLTIPAREA
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ControlPanel(state: AppState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Box(modifier = modifier.background(PanelBackground, RoundedCornerShape(12.dp))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("ИНСТРУМЕНТЫ")
            ToolButton("Добавить вершину", state.currentMode == ToolMode.ADD_VERTEX, !state.isRunning) { state.currentMode = ToolMode.ADD_VERTEX }
            ToolButton("Добавить ребро", state.currentMode == ToolMode.ADD_EDGE, !state.isRunning) { state.currentMode = ToolMode.ADD_EDGE }

            // Кнопка перемещения ВСЕГДА АКТИВНА
            ToolButton("Переместить", state.currentMode == ToolMode.MOVE, enabled = true) { state.currentMode = ToolMode.MOVE }

            ToolButton("Удалить", state.currentMode == ToolMode.DELETE, !state.isRunning) { state.currentMode = ToolMode.DELETE }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            SectionHeader("ГРАФ")

            // Кнопка загрузки с РУЧНОЙ ИКОНКОЙ И TOOLTIP
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppButton("Загрузить", modifier = Modifier.weight(1f), enabled = !state.isRunning) {
                    val file = openFileDialog()
                    if (file != null) {
                        state.loadFromFile(file)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                TooltipArea(
                    tooltip = {
                        Surface(
                            modifier = Modifier.shadow(4.dp),
                            color = Color(0xFF2A2B3D),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Формат файла (.txt):\n" +
                                        "1 строка: DIRECTED или UNDIRECTED\n" +
                                        "Далее: Откуда Куда Вес\n" +
                                        "(Количество вершин писать не нужно!)\n\n" +
                                        "Пример:\n" +
                                        "DIRECTED\n" +
                                        "A B 10\n" +
                                        "B C 5",
                                color = Color.White,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp
                            )
                        }
                    }
                ) {
                    // Кастомная нарисованная иконка "i" (не требует библиотек)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(2.dp, Color.Gray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
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

            AppButton("Пауза", enabled = false) {}

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton("◀ Назад", modifier = Modifier.weight(1f), enabled = state.isRunning) { state.stepBackward() }
                AppButton("Вперед ▶", modifier = Modifier.weight(1f), enabled = state.isRunning) { state.stepForward() }
            }

            AppButton("Авто", enabled = false) {}
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
                    // Перемещение доступно всегда, если выбран режим MOVE
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
                // Сетка
                val gridSize = 40.dp.toPx()
                val gridColor = Color.LightGray.copy(alpha = 0.4f)
                for (x in 0..size.width.toInt() step gridSize.toInt()) drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                for (y in 0..size.height.toInt() step gridSize.toInt()) drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)

                // Ребра
                state.edges.forEach { edge ->
                    val v1 = state.vertices.find { it.id == edge.from }
                    val v2 = state.vertices.find { it.id == edge.to }
                    if (v1 != null && v2 != null) {
                        drawLine(color = Color.Gray, start = Offset(v1.x, v1.y), end = Offset(v2.x, v2.y), strokeWidth = 3f)

                        // Отрисовка стрелочки для ориентированного графа
                        if (state.isDirected) {
                            val angle = atan2(v2.y - v1.y, v2.x - v1.x)
                            val radius = 22f
                            val tipX = v2.x - radius * cos(angle)
                            val tipY = v2.y - radius * sin(angle)
                            val arrowLen = 15f
                            val arrowAngle = PI / 6
                            val p1X = tipX - arrowLen * cos(angle - arrowAngle)
                            val p1Y = tipY - arrowLen * sin(angle - arrowAngle)
                            val p2X = tipX - arrowLen * cos(angle + arrowAngle)
                            val p2Y = tipY - arrowLen * sin(angle + arrowAngle)

                            val arrowPath = Path().apply {
                                moveTo(tipX, tipY)
                                lineTo(p1X.toFloat(), p1Y.toFloat())
                                lineTo(p2X.toFloat(), p2Y.toFloat())
                                close()
                            }
                            drawPath(arrowPath, Color.Gray)
                        }

                        val midX = (v1.x + v2.x) / 2
                        val midY = (v1.y + v2.y) / 2 - 20f
                        drawText(
                            textMeasurer = textMeasurer, text = edge.weight.toString(),
                            topLeft = Offset(midX, midY),
                            style = TextStyle(color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Вершины
                state.vertices.forEach { vertex ->
                    val color = if (state.selectedVertexForEdge == vertex) Color(0xFF10B981) else PrimaryBlue
                    drawCircle(color = color, radius = 22f, center = Offset(vertex.x, vertex.y))
                    drawText(
                        textMeasurer = textMeasurer, text = vertex.name,
                        topLeft = Offset(vertex.x - 8f, vertex.y - 14f),
                        style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Логгер
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF161621), RoundedCornerShape(12.dp))) {
            val logScrollState = rememberScrollState()
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp).verticalScroll(logScrollState)
            ) {
                Text("Лог выполнения:", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val msg = if (state.algorithmSteps.isNotEmpty()) state.algorithmSteps[state.currentStepIndex].logMessage else "Нарисуйте или загрузите граф..."
                Text(msg, color = Color.White, fontSize = 16.sp)
            }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(2.dp),
                adapter = rememberScrollbarAdapter(logScrollState),
                style = appScrollbarStyle()
            )
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
                val matrix = state.algorithmSteps[state.currentStepIndex].matrix
                val n = state.vertices.size
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(columns = GridCells.Fixed(n + 1), state = gridState) {
                    item { MatrixCell("", true) }
                    items(state.vertices) { v -> MatrixCell(v.name, true) }
                    for (i in 0 until n) {
                        item { MatrixCell(state.vertices[i].name, true) }
                        for (j in 0 until n) {
                            val v = matrix[i][j]
                            item { MatrixCell(if (v == INF) "∞" else v.toString(), false) }
                        }
                    }
                }

                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(2.dp),
                    adapter = rememberScrollbarAdapter(gridState),
                    style = appScrollbarStyle()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().background(Color(0xFF161621), RoundedCornerShape(8.dp)).padding(12.dp)
        ) {
            Switch(
                checked = state.isDirected,
                onCheckedChange = { state.toggleDirected() },
                enabled = !state.isRunning,
                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue, checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (state.isDirected) "Ориентированный" else "Неориентированный",
                color = Color.White, fontSize = 14.sp
            )
        }
    }
}

@Composable
fun MatrixCell(text: String, isHeader: Boolean) {
    Box(
        modifier = Modifier.aspectRatio(1f).border(1.dp, Color.DarkGray).background(if (isHeader) PanelBackground else Color.Transparent),
        contentAlignment = Alignment.Center
    ) { Text(text, color = if (isHeader) Color.Gray else Color.White, fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal) }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Floyd-Warshall Visualizer",
        state = rememberWindowState(width = 1100.dp, height = 700.dp)
    ) {
        App()
    }
}