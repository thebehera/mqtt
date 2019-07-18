package mqtt.wire

import kotlinx.io.errors.IOException

open class MqttWarning(mandatoryNormativeStatement: String, message: String) :
    IOException("$mandatoryNormativeStatement $message")
