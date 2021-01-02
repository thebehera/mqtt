// IRemoteMqttService.aidl
package com.ditchoom.mqtt;

// Declare any non-default types here with import statements
import com.ditchoom.mqtt.ControlPacketCallback;

interface IRemoteMqttService {
    void addServer();
    void removeServer();
    void connect(ControlPacketCallback connackCallback);
}