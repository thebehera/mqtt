// IRemoteMqttService.aidl
package mqtt.client;

// Declare any non-default types here with import statements
import mqtt.client.ControlPacketCallback;

interface IRemoteMqttService {
    void addServer();
    void removeServer();
    void connect(ControlPacketCallback connackCallback);
}