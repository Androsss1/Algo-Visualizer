import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ui.ControlPanel
import ui.GraphPanel
import ui.MatrixPanel

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = PanelBackground,
            primary = PrimaryBlue,
            onSurface = Color.White,
            onBackground = Color.White
        ),
        content = content
    )
}

@Composable
fun App() {
    val state = remember { AppState() }

    AppTheme {
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
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("-?\\d*"))) weightInput = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { state.confirmAddEdge(weightInput.toIntOrNull() ?: 1) }),
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