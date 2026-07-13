import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GraphPanel(state: AppState, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .weight(4f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
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
                        if (state.isRunning || state.currentMode != ToolMode.MOVE) return@pointerInput
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
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                // Сетка
                val gridSize = 40.dp.toPx()
                if (gridSize <= 0f) return@Canvas
                val gridColor = Color.LightGray.copy(alpha = 0.4f)
                for (x in 0..size.width.toInt() step gridSize.toInt()) drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), strokeWidth = 1f)
                for (y in 0..size.height.toInt() step gridSize.toInt()) drawLine(gridColor, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), strokeWidth = 1f)

                // Ребра
                state.edges.forEach { edge ->
                    val v1 = state.vertices.find { it.id == edge.from }
                    val v2 = state.vertices.find { it.id == edge.to }
                    if (v1 != null && v2 != null) {
                        drawLine(color = Color.Gray, start = Offset(v1.x, v1.y), end = Offset(v2.x, v2.y), strokeWidth = 3f)

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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF161621), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text("Лог выполнения:", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            val msg = if (state.algorithmSteps.isNotEmpty()) state.algorithmSteps[state.currentStepIndex].logMessage else "Нарисуйте граф..."
            Text(msg, color = Color.White, fontSize = 16.sp)
        }
    }
}