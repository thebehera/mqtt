package mqtt.buffer

class MalformedInvalidVariableByteInteger(value: UInt) : Exception(
    "Malformed Variable Byte Integer: This " +
            "property must be a number between 0 and %VARIABLE_BYTE_INT_MAX . Read value was: $value"
)