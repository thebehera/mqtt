package mqtt.client.service.ipc

enum class BoundClientToService {
    QUEUE_INSERTED,
    CREATE_CONNECTION,
    DISCONNECT
}

enum class ServiceToBoundClient {
    CONNECTION_ACKNOWLEGED
}