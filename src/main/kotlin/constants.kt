import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.pow
import kotlin.math.sqrt

val DarkBackground = Color(0xFF1E1E2E)
val PanelBackground = Color(0xFF252636)

// Дополнительные фоны
val LogBackground = Color(0xFF161621)
val TooltipBackground = Color(0xFF2A2B3D)

val PrimaryBlue = Color(0xFF2563EB)     
val HighlightPurple = Color(0xFF8B5CF6)
val HighlightOrange = Color(0xFFF59E0B) 
val HighlightGreen = Color(0xFF10B981)  

// Переиспользуемые формы
val CardShape = RoundedCornerShape(12.dp)
val ButtonShape = RoundedCornerShape(8.dp)
val SmallShape = RoundedCornerShape(8.dp)

const val INF = Int.MAX_VALUE / 2

@Composable
fun appScrollbarStyle() = defaultScrollbarStyle().copy(
    unhoverColor = PrimaryBlue.copy(alpha = 0.4f),
    hoverColor = PrimaryBlue,
    thickness = 8.dp
)

fun distance(x1: Float, y1: Float, x2: Float, y2: Float) = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
