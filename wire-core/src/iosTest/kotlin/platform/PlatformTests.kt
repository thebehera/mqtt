package platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformTests {
    @Test
    fun correctOS() {
        assertEquals(Platform.name, "iOS")
    }
}
