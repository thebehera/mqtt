package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class SampleTestsNative {
    @Test
    fun testHello() {
        assertTrue("NSMACHOperatingSystem" in hello())
    }
}
