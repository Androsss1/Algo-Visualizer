import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    val state = remember { AppState() }

    MaterialTheme(colorScheme = darkColorScheme(background = DarkBackground, surface = PanelBackground)) {
        Surface(modifier = Modifier.fillMaxSize().widthIn(min = 800.dp).heightIn(min = 600.dp), color = DarkBackground) {
            Row(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlPanel(state, modifier = Modifier.weight(1f).fillMaxHeight())
                GraphPanel(state, modifier = Modifier.weight(3.5f).fillMaxHeight())
                MatrixPanel(state, modifier = Modifier.weight(1.5f).fillMaxHeight())
            }
        }

        if (state.showWeightDialog) {
            var weightInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { state.cancelAddEdge() },
                title = { Text("Вес ребра", color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) weightInput = it },
                        singleLine = true,
                        placeholder = { Text("Введите число", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryBlue, unfocusedBorderColor = Color.Gray
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { state.confirmAddEdge(weightInput.toIntOrNull() ?: 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) { Text("ОК", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { state.cancelAddEdge() }) { Text("Отмена", color = Color.Gray) }
                },
                containerColor = PanelBackground
            )
        }
    }
}