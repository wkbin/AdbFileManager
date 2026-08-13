import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowSizingTest {
    @Test
    fun `uses screen-relative size on a scaled desktop`() {
        val size = calculateInitialWindowSize(1536, 864)

        assertEquals(1260, size.width)
        assertEquals(726, size.height)
    }

    @Test
    fun `never exceeds available work area on a small display`() {
        val size = calculateInitialWindowSize(800, 600)

        assertTrue(size.width <= 752)
        assertTrue(size.height <= 564)
    }

    @Test
    fun `keeps a useful minimum on a regular desktop`() {
        val size = calculateInitialWindowSize(1366, 768)

        assertTrue(size.width >= 960)
        assertTrue(size.height >= 640)
    }
}
