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
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

// Вызов нативного окна Windows/macOS для выбора файла
fun openFileDialog(): File? {
    val dialog = FileDialog(null as Frame?, "Выберите файл графа (.txt)", FileDialog.LOAD)
    dialog.file = "*.txt"
    dialog.isVisible = true
    val directory = dialog.directory
    val file = dialog.file
    return if (directory != null && file != null) File(directory, file) else null
}

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
        
        // Кнопка загрузки из файла
        AppButton("Загрузить граф", enabled = !state.isRunning) {
            val file = openFileDialog()
            if (file != null) {
                state.loadFromFile(file)
            }
        }
        
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
