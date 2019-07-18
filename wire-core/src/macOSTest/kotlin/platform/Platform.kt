package platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformTests {
    @Test
    fun testHello() {
        assertEquals("NSMACHOperatingSystem", Platform.name)
    }
}
