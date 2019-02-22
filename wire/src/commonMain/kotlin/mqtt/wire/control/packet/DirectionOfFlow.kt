package mqtt.wire.control.packet

enum class DirectionOfFlow {
    FORBIDDEN,
    CLIENT_TO_SERVER,
    SERVER_TO_CLIENT,
    BIDIRECTIONAL // Client to Server or Server to Client
}
