package mqtt.persistence

import kotlinx.browser.window

val isNodeJs by lazy {
    try {
        window
        false
    } catch (t: Throwable) {
        true
    }
}