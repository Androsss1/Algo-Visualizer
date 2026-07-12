import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

// Цвета
val DarkBackground = Color(0xFF1E1E2E)
val PanelBackground = Color(0xFF252636)
val PrimaryBlue = Color(0xFF2563EB)

@Composable
fun App() {
    MaterialTheme(colorScheme = darkColorScheme(background = DarkBackground, surface = PanelBackground)) {
        Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
            Row(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // Левая панель
                ControlPanel(modifier = Modifier.weight(1f).fillMaxHeight())

                // Центр (Граф + Логгер)
                GraphPanel(modifier = Modifier.weight(3.5f).fillMaxHeight())

                // Правая панель
                MatrixPanel(modifier = Modifier.weight(1.5f).fillMaxHeight())
            }
        }
    }
}

@Composable
fun ControlPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(PanelBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionHeader("ИНСТРУМЕНТЫ")
        AppButton("Добавить вершину") {}
        AppButton("Добавить ребро") {}
        AppButton("Переместить") {}
        AppButton("Удалить") {}

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        SectionHeader("ГРАФ")
        AppButton("Загрузить") {}
        AppButton("Очистить") {}

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        SectionHeader("АЛГОРИТМ")

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("▶ Запустить", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(8.dp) ){
                Text("Остановить",fontWeight = FontWeight.Bold, color = Color.White)
            }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton("◀ Назад", modifier = Modifier.weight(1f)) {}
            AppButton("Вперед ▶", modifier = Modifier.weight(1f)) {}
        }

        AppButton("▶▶ Авто") {}
    }
}

@Composable
fun AppButton(text: String, modifier: Modifier = Modifier.fillMaxWidth(), onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(text, fontSize = 14.sp, color = Color.White)
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun GraphPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Область ГРАФА (Белый канвас)
        Box(
            modifier = Modifier
                .weight(4f)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Область графа", color = Color.Gray)
        }

        // Область ЛОГГЕРА
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF161621), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text("Лог выполнения:", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun MatrixPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(PanelBackground, RoundedCornerShape(12.dp)).padding(16.dp)) {
        SectionHeader("МАТРИЦА РАССТОЯНИЙ")
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF161621), RoundedCornerShape(8.dp)))
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Floyd-Warshall Visualizer"
    ) {
        App()
    }
}