import platform.Platform
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformTests {
    @Test
    fun correctOS() {
        assertEquals("Linux", Platform.name)
    }
}
