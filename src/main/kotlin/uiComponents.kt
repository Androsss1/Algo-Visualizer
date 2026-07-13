import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppButton(text: String, modifier: Modifier = Modifier.fillMaxWidth(), enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)
    ) { Text(text, fontSize = 14.sp, color = Color.White) }
}

@Composable
fun ToolButton(text: String, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().height(40.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, disabledContainerColor = PrimaryBlue.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 8.dp),
        border = if (isSelected) BorderStroke(2.dp, Color.White) else null
    ) { Text(text, fontSize = 14.sp, color = Color.White) }
}

@Composable
fun SectionHeader(title: String) {
    Text(title, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 0.dp))
}

@Composable
fun MatrixCell(text: String, isHeader: Boolean) {
    Box(
        modifier = Modifier.aspectRatio(1f).border(1.dp, Color.DarkGray).background(if (isHeader) PanelBackground else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isHeader) Color.Gray else Color.White, fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal)
    }
}