@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.androidx.room

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.KModifier.OVERRIDE
import com.squareup.kotlinpoet.KModifier.SUSPEND
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import kotlin.reflect.KClass

const val mqttDbProviderGeneratorFilename = "MqttDbProvider"

fun fileSpec(
    generatedRoomDbClassName: ClassName, serializers: Collection<ClassName>,
    classNameToPublishAnnotations: Map<ClassName, MqttPublish>
) =
    FileSpec.builder(generatedRoomDbClassName.packageName, mqttDbProviderGeneratorFilename)
        .addType(classSpec(generatedRoomDbClassName, serializers, classNameToPublishAnnotations))
        .build()

fun classSpec(
    generatedRoomDbClassName: ClassName, serializers: Collection<ClassName>,
    classNameToPublishAnnotations: Map<ClassName, MqttPublish>
) =
    with(TypeSpec.objectBuilder(ClassName(generatedRoomDbClassName.packageName, mqttDbProviderGeneratorFilename))) {
        addAnnotation(ClassName("kotlinx.android.parcel", "Parcelize"))
        superclass(
            ClassName("mqtt.client.service", "MqttDatabaseDescriptor")
                .parameterizedBy(generatedRoomDbClassName)
        )
        addSuperclassConstructorParameter("%T::class.java", generatedRoomDbClassName)
        addInitializerBlock(installSerializers(serializers))
        addFunction(persistenceMethodCodeBlock())
        addFunction(subscribeMethodCodeBlock(classNameToPublishAnnotations))
        build()
    }

val androidContextClassName = ClassName("android.content", "Context")
val coroutineContextClassName = ClassName("kotlin.coroutines", "CoroutineContext")

fun persistenceMethodCodeBlock() = with(FunSpec.builder("getPersistence")) {
    addModifiers(OVERRIDE)
    addParameter("context", androidContextClassName)
    addParameter("coroutineContext", coroutineContextClassName)
    addParameter("connectionIdentifier", Int::class)
    addStatement("return ${GeneratedRoomQueuedObjectCollectionGenerator.filename}(connectionIdentifier, getDb(context), coroutineContext)")
    returns(ClassName("mqtt.client.persistence", "QueuedObjectCollection"))
    build()
}

fun subscribeMethodCodeBlock(classNameToPublishAnnotations: Map<ClassName, MqttPublish>) =
    with(FunSpec.builder("subscribe")) {
        addModifiers(OVERRIDE, SUSPEND)
        val typeVariable = TypeVariableName("T", Any::class)
        addTypeVariable(typeVariable)
        addParameter("client", ClassName("mqtt.client", "MqttClient"))
        addParameter("packetId", UShort::class)
        addParameter("topicOverride", String::class.asTypeName().copy(nullable = true))
        addParameter("qosOverride", mqttGeneratedQualityOfServiceClassName.copy(nullable = true))
        addParameter("klass", KClass::class.asTypeName().parameterizedBy(typeVariable))

        val cbParams = lambdaCallbackParams(typeVariable.copy(nullable = true))
        addParameter("cb", LambdaTypeName.get(parameters = cbParams, returnType = Unit::class.asTypeName()))
        beginControlFlow("when (klass)")
        classNameToPublishAnnotations.forEach { buildSubscribeForClass(this, it.key, it.value) }
        endControlFlow()
        build()
    }


val nameClassName = ClassName("mqtt.wire.data.topic", "Name")
fun lambdaCallbackParams(variable: TypeName) =
    listOf(
        ParameterSpec.builder("topic", nameClassName).build(),
        ParameterSpec.builder("qos", mqttGeneratedQualityOfServiceClassName).build(),
        ParameterSpec.builder("message", variable).build()
    )


val mqttGeneratedQualityOfServiceClassName = ClassName("mqtt.wire.data", "QualityOfService")

fun buildSubscribeForClass(builder: FunSpec.Builder, className: ClassName, annotation: MqttPublish) = with(builder) {
    beginControlFlow("%T::class ->", className)
    addStatement("val topicName = topicOverride ?: \"${annotation.defaultTopic}\"")
    addStatement("val qos = qosOverride ?: %T.${annotation.defaultQos}", mqttGeneratedQualityOfServiceClassName)
    addStatement("client.subscribe(topicName, qos, packetId, klass, cb)")
    endControlFlow()
}

fun installSerializers(serializers: Collection<ClassName>) = with(CodeBlock.builder()) {
    val installSerializerMethod = MemberName("mqtt.wire.control.packet", "installSerializer")
    serializers.forEach { serializerClassName ->
        addStatement("%M(%T)", installSerializerMethod, serializerClassName)
    }
    build()
}