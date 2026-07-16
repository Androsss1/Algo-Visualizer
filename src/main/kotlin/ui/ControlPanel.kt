package ui

import AppState
import ButtonShape
import CardShape
import HighlightOrange
import PanelBackground
import PrimaryBlue
import SmallShape
import TooltipBackground
import models.ToolMode
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import appScrollbarStyle
import kotlinx.coroutines.delay
import openFileDialog

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

            Box(modifier = modifier.background(PanelBackground, CardShape)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader("ИНСТРУМЕНТЫ")
            ToolButton("Добавить вершину", state.currentMode == ToolMode.ADD_VERTEX, !state.isRunning) { state.currentMode = ToolMode.ADD_VERTEX }
            ToolButton("Добавить ребро", state.currentMode == ToolMode.ADD_EDGE, !state.isRunning) { state.currentMode = ToolMode.ADD_EDGE }
            ToolButton("Переместить", state.currentMode == ToolMode.MOVE, enabled = !state.isRunning) { state.currentMode = ToolMode.MOVE }
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
                        Surface(modifier = Modifier.shadow(4.dp), color = TooltipBackground, shape = SmallShape) {
                            Text(
                                text = "Формат файла (.txt):\n1 строка: DIRECTED или UNDIRECTED\nДалее: Откуда Куда Вес\n(Количество вершин писать не нужно!)\n\nПример:\nDIRECTED\nA B 10\nB C 5",
                                color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 12.sp
                            )
                        }
                    }
                ) {
                    Box(modifier = Modifier.size(24.dp).border(2.dp, Color.Gray, CardShape), contentAlignment = Alignment.Center) {
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
                shape = ButtonShape
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
                shape = ButtonShape
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
        shape = ButtonShape, contentPadding = PaddingValues(horizontal = 8.dp)
    ) { Text(text, fontSize = 14.sp, color = Color.White) }
}

@Composable
fun ToolButton(text: String, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)),
        shape = ButtonShape, contentPadding = PaddingValues(horizontal = 8.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null
    ) { Text(text, fontSize = 14.sp, color = Color.White) }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 0.dp))
}