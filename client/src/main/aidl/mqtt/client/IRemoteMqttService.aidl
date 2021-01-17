// IRemoteMqttService.aidl
package mqtt.client;

// Declare any non-default types here with import statements
import mqtt.client.ControlPacketCallback;

interface IRemoteMqttService {
    void addServer(long connectionId, byte mqttVersion);
    void removeServer(long connectionId);
    void publish(long connectionId, long packetId, String topicName);
    void publishQos0(long connectionId, String topicName, in AssetFileDescriptor payload);
}