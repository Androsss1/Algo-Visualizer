import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ControlPanel(state: AppState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PanelBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("ИНСТРУМЕНТЫ")
        ToolButton("Добавить вершину", state.currentMode == ToolMode.ADD_VERTEX, !state.isRunning) { state.currentMode = ToolMode.ADD_VERTEX }
        ToolButton("Добавить ребро", state.currentMode == ToolMode.ADD_EDGE, !state.isRunning) { state.currentMode = ToolMode.ADD_EDGE }
        ToolButton("Переместить", state.currentMode == ToolMode.MOVE, !state.isRunning) { state.currentMode = ToolMode.MOVE }
        ToolButton("Удалить", state.currentMode == ToolMode.DELETE, !state.isRunning) { state.currentMode = ToolMode.DELETE }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

        SectionHeader("ГРАФ")
        AppButton("Загрузить граф", enabled = !state.isRunning) { state.loadExample() }
        AppButton("Очистить граф", enabled = !state.isRunning) { state.clearGraph() }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

        SectionHeader("АЛГОРИТМ")
        AppButton(if (state.isRunning) "⏹ Остановка" else "▶ Запуск", enabled = true) {
            if (state.isRunning) state.stopAlgorithm() else state.startAlgorithm()
        }

        AppButton("Пауза", enabled = false) {}

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton("◀ Назад", modifier = Modifier.weight(1f), enabled = state.isRunning) { state.stepBackward() }
            AppButton("Вперед ▶", modifier = Modifier.weight(1f), enabled = state.isRunning) { state.stepForward() }
        }

        AppButton("Авто", enabled = false) {}
    }
}