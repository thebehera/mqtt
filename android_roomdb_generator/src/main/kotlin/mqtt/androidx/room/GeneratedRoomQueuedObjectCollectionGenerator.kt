package mqtt.androidx.room

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.MemberName.Companion.member
import kotlinx.metadata.jvm.KotlinClassHeader
import kotlinx.metadata.jvm.KotlinClassMetadata
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import kotlin.coroutines.CoroutineContext

data class GeneratedRoomQueuedObjectCollectionGenerator(
    val elementUtils: Elements,
    val annotatedPublishClass: Element,
    val roomDbName: ClassName,
    val annotatedSerializerClass: TypeElement,
    val annotatedPublishDequeueElement: ExecutableElement?
) {
    val pkg = elementUtils.getPackageOf(annotatedPublishClass).qualifiedName!!.toString()


    val classSpecPersist by lazy {
        val whenBlock = listOf(
            CodeBlock.builder()
                .beginControlFlow("%T::class.java.simpleName ->", annotatedPublishClass.asType())
                // db is std convention.
                // modelsDao() is by looking at the enclosing element for @MqttPublishDequeue
                .addStatement("val obj = db.modelsDao().${annotatedPublishDequeueElement!!.simpleName}(queuedObj.queuedRowId) ?: return null")
                // validate annotatedSerializerClass is enclosing the correct class and is annotating something that returns ByteReadPacket
                .addStatement(
                    """return %T(
                    publishQueue.topic,
                    queuedObj.qos,
                    // publishable format
                    %M(obj),
                    publishQueue.packetIdentifier.toUShort(),
                    publishQueue.dup,
                    publishQueue.retain
                )""", ClassName("mqtt.wire4.control.packet", "PublishMessage"),
                    annotatedSerializerClass.asClassName().member("serialize")
                )
                .endControlFlow()
                .build()
        )

        val whenStatement = CodeBlock.builder().apply {
            addStatement("val queuedObj = nextQueuedObj(packetId) ?: return null\n")
            addStatement("val publishQueue = mqttDao.getPublishQueue(queuedObj.connectionIdentifier, queuedObj.packetIdentifier) ?: return null")
            beginControlFlow("when (queuedObj.queuedType)")
            whenBlock.forEach { add(it) }
            endControlFlow()
        }.build()
        FileSpec.builder(pkg, Companion.filename)
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class).addMember(
                    "%L",
                    "\"EXPERIMENTAL_API_USAGE\""
                ).build()
            )
            .addType(
                TypeSpec.classBuilder(Companion.filename)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addParameter("connectionIdentifier", Int::class)
                            .addParameter("db", roomDbName, OVERRIDE)
                            .addParameter("coroutineContext", CoroutineContext::class, OVERRIDE)
                            .build()
                    )
                    .addProperty(PropertySpec.builder("db", roomDbName).initializer("db").build())
                    .addProperty(
                        PropertySpec.builder(
                            "coroutineContext",
                            CoroutineContext::class
                        ).initializer("coroutineContext").build()
                    )
                    .superclass(ClassName("mqtt.client.persistence", "RoomQueuedObjectCollection"))
                    .addSuperclassConstructorParameter("db")
                    .addSuperclassConstructorParameter("connectionIdentifier")
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
                            .addCode("return null")
                            .build()
                    ).build()
            ).build()

    }

    companion object {
        const val filename = "GeneratedRoomQueuedObjectCollection"
    }
}

fun Metadata.toKotlinClassMetadata() =
    KotlinClassMetadata.read(
        KotlinClassHeader(kind, metadataVersion, bytecodeVersion, data1, data2, extraString, packageName, extraInt)
    )
