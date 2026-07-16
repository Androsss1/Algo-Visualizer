import models.Edge
import models.Vertex
import kotlin.test.*
import org.junit.Test as JvmTest

class AppStateTest {

    private fun buildState(): AppState {
        val state = AppState()
        state.addVertex(0f, 0f, "A")
        state.addVertex(100f, 0f, "B")
        state.addVertex(200f, 0f, "C")
        return state
    }

    private fun addEdge(state: AppState, fromIdx: Int, toIdx: Int, weight: Int) {
        val from = state.vertices[fromIdx]
        val to = state.vertices[toIdx]
        state.selectedVertexForEdge = from
        state.initiateEdge(from, to)
        state.confirmAddEdge(weight)
    }

    private fun distAt(state: AppState, i: Int, j: Int): Int {
        val step = state.algorithmSteps.last()
        return step.matrix[i][j]
    }

    @JvmTest
    fun `addVertex assigns sequential ids and names`() {
        val state = AppState()
        state.addVertex(10f, 20f)
        state.addVertex(30f, 40f)
        assertEquals(2, state.vertices.size)
        assertEquals(0, state.vertices[0].id)
        assertEquals(1, state.vertices[1].id)
        assertEquals("A", state.vertices[0].name)
        assertEquals("B", state.vertices[1].name)
    }

    @JvmTest
    fun `addVertex uses custom name from file loading`() {
        val state = AppState()
        state.addVertex(0f, 0f, "X1")
        assertEquals("X1", state.vertices[0].name)
    }

    @JvmTest
    fun `confirmAddEdge creates edge and updates matrix`() {
        val state = buildState()
        state.selectedVertexForEdge = state.vertices[0]
        state.initiateEdge(state.vertices[0], state.vertices[1])
        state.confirmAddEdge(5)
        assertEquals(1, state.edges.size)
        assertEquals(Edge(0, 1, 5), state.edges[0])
        assertFalse(state.showWeightDialog)
        assertNull(state.pendingEdgeTo)
    }

    @JvmTest
    fun `initiateEdge rejects duplicate edges`() {
        val state = buildState()
        addEdge(state, 0, 1, 5)
        state.selectedVertexForEdge = state.vertices[0]
        state.initiateEdge(state.vertices[0], state.vertices[1])
        assertFalse(state.showWeightDialog)
    }

    @JvmTest
    fun `initiateEdge rejects self loop`() {
        val state = buildState()
        state.initiateEdge(state.vertices[0], state.vertices[0])
        assertFalse(state.showWeightDialog)
    }

    @JvmTest
    fun `undirected graph mirrors edge both directions`() {
        val state = buildState()
        state.isDirected = false
        addEdge(state, 0, 1, 7)
        state.startAlgorithm()
        val m = state.algorithmSteps.last().matrix
        assertEquals(7, m[0][1])
        assertEquals(7, m[1][0])
    }

    @JvmTest
    fun `directed graph keeps single direction`() {
        val state = buildState()
        state.isDirected = true
        addEdge(state, 0, 1, 7)
        state.startAlgorithm()
        val m = state.algorithmSteps.last().matrix
        assertEquals(7, m[0][1])
        assertEquals(INF, m[1][0])
    }

    @JvmTest
    fun `floyd warshall finds indirect shorter path`() {
        val state = buildState()
        state.isDirected = true
        addEdge(state, 0, 1, 5)
        addEdge(state, 1, 2, 5)
        state.startAlgorithm()
        assertEquals(10, distAt(state, 0, 2))
        assertEquals(0, distAt(state, 0, 0))
        assertEquals(INF, distAt(state, 2, 0))
    }

    @JvmTest
    fun `floyd warshall chooses shortest via intermediate`() {
        val state = buildState()
        state.isDirected = true
        addEdge(state, 0, 1, 10)
        addEdge(state, 0, 2, 1)
        addEdge(state, 2, 1, 1)
        state.startAlgorithm()
        assertEquals(2, distAt(state, 0, 1))
    }

    @JvmTest
    fun `diagonal is zero after algorithm`() {
        val state = buildState()
        state.isDirected = true
        addEdge(state, 0, 1, 3)
        addEdge(state, 1, 2, 4)
        state.startAlgorithm()
        val m = state.algorithmSteps.last().matrix
        for (i in m.indices) assertEquals(0, m[i][i])
    }

    @JvmTest
    fun `unreachable vertices stay INF`() {
        val state = buildState()
        state.isDirected = true
        addEdge(state, 0, 1, 3)
        state.startAlgorithm()
        assertEquals(INF, distAt(state, 1, 0))
        assertEquals(INF, distAt(state, 2, 0))
    }

    @JvmTest
    fun `startAlgorithm produces multiple steps`() {
        val state = buildState()
        state.isDirected = true
        addEdge(state, 0, 1, 3)
        addEdge(state, 1, 2, 4)
        state.startAlgorithm()
        assertTrue(state.algorithmSteps.size > 1)
        assertEquals(0, state.currentStepIndex)
    }

    @JvmTest
    fun `step navigation respects bounds`() {
        val state = buildState()
        state.isDirected = true
        addEdge(state, 0, 1, 3)
        state.startAlgorithm()
        val last = state.algorithmSteps.size - 1
        assertEquals(0, state.currentStepIndex)
        repeat(100) { state.stepForward() }
        assertEquals(last, state.currentStepIndex)
        state.stepForward()
        assertEquals(last, state.currentStepIndex)
        state.stepBackward()
        assertEquals(last - 1, state.currentStepIndex)
        repeat(100) { state.stepBackward() }
        assertEquals(0, state.currentStepIndex)
    }

    @JvmTest
    fun `toggleAutoPlay sets playing flag`() {
        val state = buildState()
        state.isDirected = true
        addEdge(state, 0, 1, 3)
        state.startAlgorithm()
        state.stepForward()
        state.toggleAutoPlay()
        assertEquals(true, state.isAutoPlaying)
    }

    @JvmTest
    fun `moveVertex shifts coordinates`() {
        val state = buildState()
        val oldX = state.vertices[0].x
        state.moveVertex(0, 10f, 20f)
        assertEquals(oldX + 10f, state.vertices[0].x)
    }

    @JvmTest
    fun `removeElement deletes vertex and connected edges`() {
        val state = buildState()
        addEdge(state, 0, 1, 5)
        assertEquals(1, state.edges.size)
        state.removeElement(state.vertices[0].x, state.vertices[0].y)
        assertEquals(2, state.vertices.size)
        assertEquals(0, state.edges.size)
    }

    @JvmTest
    fun `removeElement deletes edge when clicking near it`() {
        val state = buildState()
        addEdge(state, 0, 1, 5)
        val v1 = state.vertices[0]
        val v2 = state.vertices[1]
        state.removeElement((v1.x + v2.x) / 2, (v1.y + v2.y) / 2)
        assertEquals(0, state.edges.size)
        assertEquals(3, state.vertices.size)
    }

    @JvmTest
    fun `clearGraph resets everything`() {
        val state = buildState()
        addEdge(state, 0, 1, 5)
        state.isRunning = true
        state.clearGraph()
        assertEquals(0, state.vertices.size)
        assertEquals(0, state.edges.size)
        assertEquals(false, state.isRunning)
    }

    @JvmTest
    fun `toggleDirected blocked while running`() {
        val state = buildState()
        state.isRunning = true
        val before = state.isDirected
        state.toggleDirected()
        assertEquals(before, state.isDirected)
    }

    @JvmTest
    fun `vertexIndexById returns correct index`() {
        val state = buildState()
        assertEquals(2, state.vertexIndexById(state.vertices[2].id))
        assertEquals(-1, state.vertexIndexById(999))
    }

    @JvmTest
    fun `loadFromFile parses directed graph`() {
        val state = AppState()
        val file = createTempFile("graph", ".txt").apply {
            writeText(
                """
                DIRECTED
                A B 3
                B C 4
                """.trimIndent()
            )
        }
        state.loadFromFile(file)
        assertEquals(3, state.vertices.size)
        assertEquals(2, state.edges.size)
        state.startAlgorithm()
        assertEquals(7, distAt(state, 0, 2))
    }

    @JvmTest
    fun `loadFromFile rejects bad header`() {
        val state = AppState()
        val file = createTempFile("graph", ".txt").apply {
            writeText("GRAPH\nA B 1")
        }
        state.loadFromFile(file)
        assertTrue(state.algorithmSteps.first().logMessage.contains("Ошибка"))
    }

    @JvmTest
    fun `loadFromFile rejects malformed edge line`() {
        val state = AppState()
        val file = createTempFile("graph", ".txt").apply {
            writeText("UNDIRECTED\nA B")
        }
        state.loadFromFile(file)
        assertTrue(state.algorithmSteps.first().logMessage.contains("Ошибка"))
    }

    @JvmTest
    fun `updateInitialMatrix never runs during algorithm`() {
        val state = buildState()
        addEdge(state, 0, 1, 5)
        state.isRunning = true
        val before = state.algorithmSteps
        state.moveVertex(0, 1f, 1f)
        assertEquals(before, state.algorithmSteps)
    }
}
