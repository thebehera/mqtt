package sample

import platform.Foundation.NSProcessInfo

actual class Sample {
    actual fun checkMe() = 7
}

actual object Platform {
    actual val name: String = NSProcessInfo.processInfo().operatingSystemName()
}
