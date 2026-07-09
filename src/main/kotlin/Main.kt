import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color


@Composable
fun App(){
    MaterialTheme {
        Row(modifier = Modifier.fillMaxSize()) {
            // панелька для контроля алгоритма(кнопки назад,вперед)
            ControlPanel(modifier = Modifier.weight(1f).fillMaxHeight())
            // рабочая область
            GraphPanel(modifier = Modifier.weight(3.5f).fillMaxHeight())
            // матрица
            MatrixPanel(modifier = Modifier.weight(2f).fillMaxHeight())
        }
    }

}

@Composable
fun ControlPanel(modifier: Modifier = Modifier){
    Box(modifier = modifier.background(Color.DarkGray).padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize(),horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.spacedBy(16.dp))
        {
            Text("Кнопки",color = Color.Black)

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Добавить вершину")
            }


            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Добавить ребро")
            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Переместить")
            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Удалить")
            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Загрузить граф")
            }


            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Очистить")
            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("запустить")
            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ){
                Text("Остановить")
            }

            Button(
                onClick = { /* TODO: step backward */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("◀ Назад")
            }

            Button(
                onClick = { /* TODO: step forward */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Вперёд ▶")
            }

            Button(
                onClick = { /* TODO: auto play */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("▶▶ Авто")
            }


            Spacer(modifier = Modifier.weight(1f))

            // Кнопки работы с файлами
            Button(
                onClick = { /* TODO: load */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Загрузить")
            }


        }
    }
}


@Composable
fun GraphPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Color(0xFFF5F5F5))) {

        // СЕКЦИЯ 1: Граф
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("Граф", style = MaterialTheme.typography.headlineMedium)
        }

        // Разделитель
        HorizontalDivider(thickness = 2.dp, color = Color.Gray)

        // СЕКЦИЯ 2: Лог
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Color(0xFF263238))
                .padding(8.dp)
        ) {
            Column {
                // Заголовок лога
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Лог выполнения", color = Color.White)
                    Text("Очистить", color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


@Composable
fun MatrixPanel(modifier: Modifier = Modifier){
    Box(modifier = modifier){
        Text("Матрица расстояний")
    }
}



fun main() = application {

    Window(onCloseRequest = ::exitApplication,title = "Floyd-Warshall algorithm visualizer")
    {
        App()
    }
}


