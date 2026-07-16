package ui

import AppState
import ButtonShape
import CardShape
import HighlightOrange
import HighlightPurple
import INF
import LogBackground
import PanelBackground
import PrimaryBlue
import SmallShape
import models.Edge
import models.ToolMode
import models.Vertex
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import appScrollbarStyle

@Composable
fun MatrixPanel(state: AppState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(PanelBackground, CardShape).padding(16.dp)) {
        SectionHeader("МАТРИЦА РАССТОЯНИЙ")
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(LogBackground, SmallShape).padding(8.dp)) {
            if (state.vertices.isEmpty()) {
                Text("Граф пуст", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else if (state.algorithmSteps.isNotEmpty()) {
                val step = state.algorithmSteps[state.currentStepIndex]
                val matrix = step.matrix
                val n = state.vertices.size
                val prevMatrix = if (state.currentStepIndex > 0) state.algorithmSteps[state.currentStepIndex - 1].matrix else null
                val gridState = rememberLazyGridState()

                LazyVerticalGrid(columns = GridCells.Fixed(n + 1), state = gridState) {
                    item { MatrixCell("", isHeader = true, bgColor = PanelBackground) }

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
                        val hBg = when (i) {
                            step.activeI -> PrimaryBlue
                            step.activeJ -> HighlightPurple
                            step.activeK -> HighlightOrange
                            else -> PanelBackground
                        }
                        item { MatrixCell(state.vertices[i].name, isHeader = true, bgColor = hBg) }

                        for (j in 0 until n) {
                            val v = matrix[i][j]
                            val isTarget = (i == step.activeI && j == step.activeJ)
                            val isComponent = if (step.activeK != null) {
                                (i == step.activeI && j == step.activeK) || (i == step.activeK && j == step.activeJ)
                            } else false
                            val changed = prevMatrix != null && prevMatrix[i][j] != v

                            val bgColor = if (isComponent) HighlightOrange.copy(alpha = 0.5f) else Color.Transparent
                            val borderColor = if (isTarget) HighlightPurple else Color.DarkGray
                            val borderThickness = if (isTarget) 3.dp else 1.dp

                            item { MatrixCell(if (v == INF) "∞" else v.toString(), isHeader = false, bgColor = bgColor, borderColor = borderColor, borderWidth = borderThickness, changed = changed) }
                        }
                    }
                }
                VerticalScrollbar(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(2.dp), adapter = rememberScrollbarAdapter(gridState), style = appScrollbarStyle())
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(LogBackground, SmallShape).padding(12.dp)) {
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
fun MatrixCell(text: String, isHeader: Boolean, bgColor: Color, borderColor: Color = Color.DarkGray, borderWidth: Dp = 1.dp, changed: Boolean = false) {
    Box(
        modifier = Modifier.aspectRatio(1f).border(borderWidth, borderColor).background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = when {
                changed -> Color.Green
                isHeader && bgColor == PanelBackground -> Color.Gray
                else -> Color.White
            },
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
        )
    }
}
