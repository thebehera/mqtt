package mqtt.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SampleTests {

    @Test
    fun testSampleTestValue() {
        assertEquals("hello", test)
    }

    @Test
    fun twoPlusTwoUsingNativeClassWorks() {
        assertEquals(2 + 2, SampleNativeClass(2).twoPlusTwo())
    }

    @Test
    fun twoPlusTwoUsingNativeWorksFailsCorrectly() {
        assertNotEquals(2 + 3, SampleNativeClass(4).twoPlusTwo())
    }

    @Test
    fun valuePassedWorks() {
        assertEquals(4, SampleNativeClass(4).x)
    }


    @Test
    fun valuePassedWorksFails() {
        assertNotEquals(5, SampleNativeClass(4).x)
    }
}