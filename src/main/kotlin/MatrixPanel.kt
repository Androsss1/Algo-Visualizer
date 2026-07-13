import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MatrixPanel(state: AppState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PanelBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        SectionHeader("МАТРИЦА РАССТОЯНИЙ")
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF161621), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (state.vertices.isEmpty()) {
                Text("Граф пуст", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
            } else if (state.algorithmSteps.isNotEmpty()) {
                val matrix = state.algorithmSteps[state.currentStepIndex].matrix
                val n = state.vertices.size
                LazyVerticalGrid(columns = GridCells.Fixed(n + 1)) {
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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161621), RoundedCornerShape(8.dp))
                .padding(12.dp)
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