package platform

import platform.UIKit.UIDevice

actual object Platform {
    actual val name: String = UIDevice.currentDevice.systemName()
}
