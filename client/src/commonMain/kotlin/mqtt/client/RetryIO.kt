package mqtt.client

import kotlinx.coroutines.delay
import kotlinx.io.errors.IOException
import mqtt.wire.BrokerRejectedConnection
import mqtt.wire.ProtocolError

suspend fun retryIO(
        times: Int = Int.MAX_VALUE,
        initialDelay: Long = 100, // 0.1 second
        maxDelay: Long = 1000,    // 1 second
        factor: Double = 2.0,
        stopConnectingOnProtocolError: Boolean = true,
        stopConnectingOnServerRejectedConnection: Boolean = true,
        block: suspend () -> Unit) {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            block()
        } catch (e: BrokerRejectedConnection) {
            println("Server rejected our connection: $e")
            if (stopConnectingOnServerRejectedConnection) {
                return
            }
        } catch (e: ProtocolError) {
            println("Protocol error stopping now $e")
            if (stopConnectingOnProtocolError) {
                return
            }
        } catch (e: IOException) {
            println("IOException retrying in $currentDelay ms $e")
        } catch (e: Exception) {
            // you can log an error here and/or make a more finer-grained
            // analysis of the cause to see if retry is needed
            println("error while retrying: $e")
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}
