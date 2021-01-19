// IRemoteMqttService.aidl
package mqtt.client;

// Declare any non-default types here with import statements
import mqtt.client.ControlPacketCallback;

interface IRemoteMqttService {
    void addServer(long connectionId, byte mqttVersion);
    void removeServer(long connectionId);
    void publish(long connectionId, long packetId);
    void publishQos0Fd(long connectionId, String topicName, in AssetFileDescriptor payload);
    void publishQos0(long connectionId, String topicName, in byte[] payload);
}