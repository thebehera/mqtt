package mqtt.persistence

fun isNodeJs(): Boolean {
    return try {
        !(js("'WebSocket' in window || 'MozWebSocket' in window") as Boolean)
    } catch (t: Throwable) {
        true
    }
}