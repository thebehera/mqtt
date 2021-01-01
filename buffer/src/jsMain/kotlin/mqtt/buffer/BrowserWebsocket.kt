package mqtt.buffer

import org.w3c.dom.WebSocket
import org.w3c.dom.events.Event
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BrowserWebsocket(val url: String) {
    val websocket = WebSocket(url, "mqtt")
    var isOpen: Boolean = false
    suspend fun open() {
        websocket.onerror = {
            console.error("WS error:", it)
        }
        suspendCoroutine<Event> {
            websocket.onopen = { event ->
                isOpen = true
                it.resume(event)
            }
        }
        websocket.onclose = {
            this.isOpen = false
            Unit
        }
    }

    fun write(buffer: PlatformBuffer) {
        buffer.resetForRead()
        val jsBuffer = buffer as JsBuffer
        val sliced = jsBuffer.buffer.buffer.slice(buffer.position().toInt(), buffer.limit().toInt())
        websocket.send(sliced)
    }

    fun read(cb: (PlatformBuffer) -> Unit) {
        websocket.onmessage = {

        }
    }

    fun close() {
        websocket.close()
    }
}