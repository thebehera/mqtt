package mqtt.wire


open class MqttWarning(mandatoryNormativeStatement: String, message: String) :
    Exception("$mandatoryNormativeStatement $message")
