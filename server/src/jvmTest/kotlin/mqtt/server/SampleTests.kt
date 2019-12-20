package mqtt.server

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SampleJVMTests {

    @Test
    fun testSampleTestValue() {
        assertEquals("hello", test)
    }

    @Test
    fun twoPlusTwoUsingJVMBigIntegerClassWorks() {
        assertEquals((BigInteger.valueOf(2) + BigInteger.valueOf(2)).toInt(), SampleNativeClass(2).twoPlusTwo())
    }

    @Test
    fun twoPlusTwoUsingJVMBigIntegerWorksFailsCorrectly() {
        assertNotEquals((BigInteger.valueOf(2) + BigInteger.valueOf(3)).toInt(), SampleNativeClass(4).twoPlusTwo())
    }

    @Test
    fun valuePassedWorks() {
        assertEquals(BigInteger.valueOf(4).toInt(), SampleNativeClass(4).x)
    }


    @Test
    fun valuePassedWorksFails() {
        assertNotEquals(BigInteger.valueOf(5).toInt(), SampleNativeClass(4).x)
    }
}