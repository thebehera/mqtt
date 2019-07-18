package platform

import platform.Foundation.NSProcessInfo

actual object Platform {
    actual val name: String = NSProcessInfo.processInfo().operatingSystemName()
}
