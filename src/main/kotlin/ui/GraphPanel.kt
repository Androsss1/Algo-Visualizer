package ui

import AppState
import ButtonShape
import CardShape
import HighlightGreen
import HighlightOrange
import HighlightPurple
import LogBackground
import PrimaryBlue
import SmallShape
import models.Edge
import models.ToolMode
import models.Vertex
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import appScrollbarStyle
import distance
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GraphPanel(state: AppState, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.weight(4f).fillMaxWidth().background(Color.White, CardShape)) {
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
                    val v1Idx = state.vertexIndexById(edge.from)
                    val v2Idx = state.vertexIndexById(edge.to)
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

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(LogBackground, CardShape)) {
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