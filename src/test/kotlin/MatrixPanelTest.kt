import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import kotlin.test.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ui.MatrixPanel

class MatrixPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var state: AppState

    @Before
    fun setup() {
        state = AppState()
        state.addVertex(0f, 0f, "A")
        state.addVertex(100f, 0f, "B")
        state.addVertex(200f, 0f, "C")
    }

    private fun show(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            Box(Modifier.requiredSize(600.dp)) { content() }
        }
    }

    @Test
    fun `renders empty graph message when no vertices`() {
        show { MatrixPanel(AppState()) }
        composeTestRule.onNodeWithText("Граф пуст").assertIsDisplayed()
    }

    @Test
    fun `renders matrix headers with vertex names`() {
        show { MatrixPanel(state) }
        composeTestRule.onNodeWithText("A").assertExists()
        composeTestRule.onNodeWithText("B").assertExists()
        composeTestRule.onNodeWithText("C").assertExists()
        composeTestRule.onNodeWithText("МАТРИЦА РАССТОЯНИЙ").assertIsDisplayed()
    }

    @Test
    fun `shows infinity symbol for unreachable cells before run`() {
        show { MatrixPanel(state) }
        composeTestRule.onNodeWithText("∞").assertExists()
    }

    @Test
    fun `directed switch toggles label text`() {
        state.isDirected = false
        show { MatrixPanel(state) }
        composeTestRule.onNodeWithText("Неориентированный").assertIsDisplayed()
        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.onNodeWithText("Ориентированный").assertIsDisplayed()
    }

    @Test
    fun `updated cell highlighted value after step change`() {
        state.isDirected = true
        state.selectedVertexForEdge = state.vertices[0]
        state.initiateEdge(state.vertices[0], state.vertices[1])
        state.confirmAddEdge(5)
        state.startAlgorithm()
        show { MatrixPanel(state) }
        composeTestRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun `matrix reflects algorithm result after running`() {
        state.isDirected = true
        state.selectedVertexForEdge = state.vertices[0]
        state.initiateEdge(state.vertices[0], state.vertices[1])
        state.confirmAddEdge(5)
        state.selectedVertexForEdge = state.vertices[1]
        state.initiateEdge(state.vertices[1], state.vertices[2])
        state.confirmAddEdge(5)
        state.startAlgorithm()
        show { MatrixPanel(state) }
        composeTestRule.onNodeWithText("10").assertExists()
    }
}
