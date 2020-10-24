package mqtt.persistence

import kotlinx.browser.window

class JsDatabase {
    fun x() {

    }
}

val isNodeJs by lazy {
    try {
        window
        false
    } catch (t: Throwable) {
        true
    }
}