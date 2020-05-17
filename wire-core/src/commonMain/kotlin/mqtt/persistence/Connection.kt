package mqtt.persistence

import mqtt.connection.IRemoteHost

interface PersistableConnectionManager {
    fun addOrUpdateConnection(remoteHost: IRemoteHost)
}
