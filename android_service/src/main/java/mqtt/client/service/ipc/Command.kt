package mqtt.client.service.ipc

enum class BoundClientToService(val position: Int) {
    CREATE_CONNECTION(1),
    DISCONNECT(2),
    QUEUE_INSERTED(3),
    SUBSCRIBE(4),
    UNSUBSCRIBE(5)
}

enum class ServiceToBoundClient(val position: Int) {
    CONNECTION_STATE_CHANGED(1),
    INCOMING_CONTROL_PACKET(2),
    OUTGOING_CONTROL_PACKET(3)
}


const val rowIdKey = "rowId"
const val tableNameKey = "tableName"