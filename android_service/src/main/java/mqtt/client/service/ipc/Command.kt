package mqtt.client.service.ipc

enum class BoundClientToService(val position: Int) {
    QUEUE_INSERTED(1),
    CREATE_CONNECTION(2),
    DISCONNECT(3)
}

enum class ServiceToBoundClient(val position: Int) {
    CONNECTION_STATE_CHANGED(1),
    INCOMING_CONTROL_PACKET(2),
    OUTGOING_CONTROL_PACKET(3)
}


const val rowIdKey = "rowId"
const val tableNameKey = "tableName"