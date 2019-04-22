//package mqtt.client

//import kotlin.test.Test

//class ConnectionJSTests {

//    @Test
//    fun main() {
//        val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort(), hasUserName = true, hasPassword = true)
//        val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"), userName = MqttUtf8String("hktkqtso"), password = MqttUtf8String("C39Mn5EQYQQZ"))
//        val params = ConnectionParameters("m16.cloudmqtt.com", 22655, true, ConnectionRequest(header, payload), reconnectIfNetworkLost = false)
////    val header = ConnectionRequest.VariableHeader(keepAliveSeconds = 5.toUShort())
////    val payload = ConnectionRequest.Payload(clientId = MqttUtf8String("JavaSample"))
////    val params = ConnectionParameters("localhost", 60000,false, ConnectionRequest(header, payload), reconnectIfNetworkLost = true)
//
//        val connection = openConnection(params)
//        runBlocking {
//            //        val fixed = PublishMessage.FixedHeader(qos = QualityOfService.AT_LEAST_ONCE)
////        val variable = PublishMessage.VariableHeader(MqttUtf8String("yolo"), 1.toUShort())
////        delay(1000)
////        params.clientToBroker.send(PublishMessage(fixed, variable))
////            for (inMessage in params.brokerToClient) {
////                println("IN: $inMessage")
////            }
//            params.clientToBroker.send(DisconnectNotification)
//            println("await")
//            connection.await()
//            println("await complete")
//            val exception = connection.getCompletionExceptionOrNull()
//            println("exception: $exception")
//            println("Completion: ${connection.getCompleted()}")
//        }
//    }
//
//}
