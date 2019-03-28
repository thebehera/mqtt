package platform

import kotlin.test.Test
import kotlin.test.assertNotNull

class PlatformCommonTests {
    @Test
    fun testNotNull() {
        assertNotNull(Platform.name)
    }
}
