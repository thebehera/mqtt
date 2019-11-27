package mqtt.androidx.room

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.SUSPEND
import javax.lang.model.element.Element
import javax.lang.model.util.Elements
import kotlin.coroutines.CoroutineContext

data class GeneratedRoomQueuedObjectCollectionGenerator(
    val elementUtils: Elements,
    val annotatedPublishClass: Element,
    val roomDbName: ClassName,
    val mqttPublish: MqttPublish,
    val annotatedPublishPacketFunction: MqttPublishPacket,
    val annotatedDequeFunctionElement: MqttPublishDequeue?
) {
    val pkg = elementUtils.getPackageOf(annotatedPublishClass).qualifiedName!!.toString()
    val filename = "GeneratedRoomQueuedObjectCollection"

    val classSpec by lazy {
        println("$annotatedPublishClass $annotatedDequeFunctionElement, $annotatedPublishPacketFunction")
        val whenBlock = listOf(
            CodeBlock.builder()
                .beginControlFlow(
                    "%T::class.java.simpleName ->",
                    ClassName(pkg, annotatedPublishClass.simpleName.toString())
                )
                // How do we pick up getByRowId?? for dequeing
                .add("val obj = db.modelsDao().getByRowId(queuedObj.queuedRowId) ?: return null")
                // how do we get 'obj.toByteReadPacket(),'?? don't we need to import this?
                .add(
                    """return PublishMessage(
                    publishQueue.topic,
                    queuedObj.qos,
                    // publishable format
                    obj.toByteReadPacket(),
                    publishQueue.packetIdentifier.toUShort(),
                    publishQueue.dup,
                    publishQueue.retain
                )"""
                )
                .endControlFlow()
                .build()
        )

        val whenStatement = CodeBlock.builder().apply {
            add("val queuedObj = nextQueuedObj(packetId) ?: return null")
            add("val publishQueue = mqttDao.getPublishQueue(queuedObj.connectionIdentifier, queuedObj.packetIdentifier) ?: return null")
            beginControlFlow("when (queuedObj.queuedType)")
            whenBlock.forEach { add(it) }
            endControlFlow()
        }.build()
        FileSpec.builder(pkg, filename)
            .addType(
                TypeSpec.classBuilder(filename).addProperty("connectionIdentifier", Int::class.java)
                    .addProperty("db", roomDbName, OVERRIDE)
                    .addProperty("coroutineContext", CoroutineContext::class, OVERRIDE)
                    .superclass(ClassName("mqtt.client.persistence", "RoomQueuedObjectCollection"))
                    .addSuperclassConstructorParameter("db")
                    .addSuperclassConstructorParameter("connectionIdentifier").build()
            )
            .addProperty(
                PropertySpec.builder(
                    "mqttDao",
                    ClassName("mqtt.client.persistence", "PersistedMqttQueueDao")
                )
                    .initializer("db.mqttQueueDao()").build()
            )
            .addFunction(
                FunSpec.builder("get")
                    .addModifiers(OVERRIDE, SUSPEND)
                    .addParameter("packetId", Int::class.asTypeName().copy(true))
                    .returns(ClassName("mqtt.wire.control.packet", "ControlPacket").copy(true))
                    .addCode(
                        CodeBlock.builder()
                            .add(whenStatement)
                            .build()
                    )
                    .build()
            ).build()

    }
}