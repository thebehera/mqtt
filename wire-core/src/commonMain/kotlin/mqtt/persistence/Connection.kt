package mqtt.persistence

import mqtt.Parcelable
import mqtt.connection.IRemoteHost

interface PersistableConnectionManager : Parcelable {
    fun addOrUpdateConnection(remoteHost: IRemoteHost)
}
