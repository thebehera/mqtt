package mqtt.androidx.room

import com.squareup.kotlinpoet.*
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

fun fileSpec(
    packageName: String,
    classNameToPublishAnnotations: Map<ClassName, MqttPublish>,
    classNameToPublishQueueAnnotations: Map<ClassName, PublishQueueParams>
) = with(FileSpec.builder(packageName, "MqttServiceViewModelGenerated")) {
    addType(classSpec(packageName, classNameToPublishAnnotations, classNameToPublishQueueAnnotations))
    build()
}

fun classSpec(
    packageName: String, classNameToPublishAnnotations: Map<ClassName, MqttPublish>,
    classNameToPublishQueueAnnotations: Map<ClassName, PublishQueueParams>
) = with(TypeSpec.classBuilder("MqttServiceViewModelGenerated")) {
    val androidApplicationClassName = ClassName("android.app", "Application")
    val applicationParameter = FunSpec.constructorBuilder().addParameter("app", androidApplicationClassName).build()
    primaryConstructor(applicationParameter)
    addProperty(
        PropertySpec.builder("app", androidApplicationClassName)
            .initializer("app")
            .addModifiers(KModifier.PRIVATE)
            .build()
    )
    superclass(ClassName("mqtt.client.service", "AbstractMqttServiceViewModel"))

    addAnnotation(ClassName("kotlin.time", "ExperimentalTime"))
    addSuperclassConstructorParameter("app")
    addSuperclassConstructorParameter("%T", ClassName(packageName, mqttDbProviderGeneratorFilename))
    addFunction(subscribeSpec(classNameToPublishAnnotations))
    classNameToPublishQueueAnnotations.forEach { addFunction(publishSpec(it.key, it.value)) }
    build()
}

data class PublishQueueParams(
    val insertMethod: ExecutableElement,
    val publishAnnotation: MqttPublish,
    val publishQueueAnnotation: MqttPublishQueue,
    val databaseClassAnnotation: TypeElement
) {
    val daoMethod by lazy {
        val dao = insertMethod.enclosingElement as TypeElement
        databaseClassAnnotation
            .enclosedElements
            .filterIsInstance<ExecutableElement>()
            .first { it.returnType.asTypeName() == dao.asClassName() }
    }
}

fun publishSpec(annotatedClassName: ClassName, publishQueue: PublishQueueParams) = with(FunSpec.builder("publish")) {
    addModifiers(KModifier.SUSPEND)
    val typeVariable = TypeVariableName("T", Any::class)
    addTypeVariable(typeVariable)
    addParameter("connectionIdentifier", Int::class)
    addParameter("obj", typeVariable)
    addParameter(
        ParameterSpec.builder("topicOverride", String::class.asTypeName().copy(nullable = true))
            .defaultValue("null").build()
    )
    addParameter(
        ParameterSpec.builder("qosOverride", mqttGeneratedQualityOfServiceClassName.copy(nullable = true))
            .defaultValue("null").build()
    )
    addParameter(
        ParameterSpec.builder("dupOverride", Boolean::class.asTypeName().copy(nullable = true))
            .defaultValue("null").build()
    )
    addParameter(
        ParameterSpec.builder("retainOverride", Boolean::class.asTypeName().copy(nullable = true))
            .defaultValue("null").build()
    )

    addStatement("val db = %T.getDb(app)", ClassName(annotatedClassName.packageName, mqttDbProviderGeneratorFilename))
    beginControlFlow("val rowId = when (obj)")
    publishWhenForType(annotatedClassName, publishQueue)
    beginControlFlow("else ->")
    addStatement(
        "throw %T(\"Failed to publish \$obj. Did you forget to annotate\" +  \"\${obj::class.java.canonicalName} with @MqttPublish?)\")",
        MqttGeneratedCodeException::class
    )
    endControlFlow()
    endControlFlow()
    addStatement(
        "notifyPublish(%T(connectionIdentifier, rowId))",
        ClassName("mqtt.client.service.ipc.ClientToServiceConnection", "NotifyPublish")
    )
    build()
}


fun FunSpec.Builder.publishWhenForType(className: ClassName, params: PublishQueueParams) {
    beginControlFlow("is %T ->", className)
    addStatement("val id = db.${params.daoMethod.simpleName}().%L(obj)", params.insertMethod.simpleName)
    addStatement("val modelWithId = obj.copy(key = id)")
    addStatement(
        "val qos = qosOverride ?: %T.${params.publishAnnotation.defaultQos}",
        mqttGeneratedQualityOfServiceClassName
    )
    addStatement(
        "val queue = %T(%T::class.java.simpleName, modelWithId.key, %T.controlPacketValue, qos, connectionIdentifier)",
        ClassName("mqtt.client.persistence", "MqttQueue"), className,
        ClassName("mqtt.wire.control.packet", "IPublishMessage")
    )
    addStatement("val topic = topicOverride ?: %S", params.publishAnnotation.defaultTopic)
    addStatement("val dup = dupOverride ?: ${params.publishAnnotation.defaultDup}")
    addStatement("val retain = retainOverride ?: ${params.publishAnnotation.defaultRetain}")
    addStatement("db.mqttQueueDao().publish(queue, topic, dup, retain)")
    endControlFlow()
}

fun subscribeSpec(classNameToPublishAnnotations: Map<ClassName, MqttPublish>) = with(FunSpec.builder("subscribe")) {
    addModifiers(KModifier.SUSPEND, KModifier.INLINE)
    val typeVariable = TypeVariableName("T", listOf(Any::class.asClassName())).copy(reified = true)
    addTypeVariable(typeVariable)
    addParameter("connectionIdentifier", Int::class)
    addParameter(
        ParameterSpec.builder("topicOverride", String::class.asTypeName().copy(nullable = true))
            .defaultValue("null").build()
    )
    addParameter(
        ParameterSpec.builder("qosOverride", mqttGeneratedQualityOfServiceClassName.copy(nullable = true))
            .defaultValue("null").build()
    )

    val cbParams = lambdaCallbackParams(typeVariable.copy(nullable = true))
    addParameter(
        "cb",
        LambdaTypeName.get(parameters = cbParams, returnType = Unit::class.asTypeName()),
        KModifier.CROSSINLINE
    )

    addCode(
        """
        val subscriptionCallback = object : %T<T> {
            override fun onMessageReceived(topic: %T, qos: %T, message: T?) {
                cb(topic, qos, message)
            }
        }
        
    """.trimIndent(),
        ClassName("mqtt.wire.data.topic", "SubscriptionCallback"),
        nameClassName, mqttGeneratedQualityOfServiceClassName
    )
    whenStatement(this, classNameToPublishAnnotations)
    build()
}

fun whenStatement(builder: FunSpec.Builder, classNameToPublishAnnotations: Map<ClassName, MqttPublish>) =
    with(builder) {
        beginControlFlow("when (T::class)")
        classNameToPublishAnnotations.forEach { buildSubscribeForViewModel(this, it.key, it.value) }
        endControlFlow()
        build()
    }

fun buildSubscribeForViewModel(builder: FunSpec.Builder, className: ClassName, annotation: MqttPublish) =
    with(builder) {
        beginControlFlow("%T::class ->", className)
        addStatement("val topicName = topicOverride ?: \"${annotation.defaultTopic}\"")
        addStatement("val qos = qosOverride ?: %T.${annotation.defaultQos}", mqttGeneratedQualityOfServiceClassName)
        addStatement("subscribe(topicName, qos, connectionIdentifier, subscriptionCallback)")
        endControlFlow()
    }