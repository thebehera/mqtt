package sample

import kotlin.test.Test
import kotlin.test.assertTrue

class SampleTestsios {
    @Test
    fun testHello() {
        assertTrue("Native" in hello())
    }
}
