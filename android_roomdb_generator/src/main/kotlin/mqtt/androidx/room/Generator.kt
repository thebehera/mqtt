package mqtt.androidx.room

import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

@AutoService(Processor::class)
class MqttCodeGenerator : AbstractProcessor() {
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private val dbClassRef = MqttDatabase::class.java
    private val publishRef = MqttPublish::class.java
    private val publishDequeRef = MqttPublishDequeue::class.java
    private val publishPacketRef = MqttPublishPacket::class.java

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils!!
        elementUtils = processingEnv.elementUtils!!
        filer = processingEnv.filer!!
        messager = processingEnv.messager
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val databases = roundEnv.getElementsAnnotatedWith(dbClassRef).map {
            val annotation = it.getAnnotation(dbClassRef)
            JavaAnnotatedMqttElement(processingEnv, typeUtils, elementUtils, it, annotation)
        }
//        val publishModels = roundEnv.getElementsAnnotatedWith(publishRef).map { it!! }.first()
//        val publishPackets = roundEnv.getElementsAnnotatedWith(publishPacketRef).map { it!!}.first()
//        val publishDeque = roundEnv.getElementsAnnotatedWith(publishDequeRef).map {
//            GeneratedRoomQueuedObjectCollectionGenerator(elementUtils, publishModels, ClassName("mqtt.android_app", "Mqtt_RoomDb_SimpleModelDb"), it, publishPackets)
//        }
//        publishDeque.toString()
        databases.forEach { it.write(filer) }
        return true
    }

    override fun getSupportedAnnotationTypes() =
        setOf(
            MqttDatabase::class.qualifiedName!!,
            MqttPublish::class.qualifiedName!!,
            MqttPublishDequeue::class.qualifiedName!!,
            MqttPublishPacket::class.qualifiedName!!
        )

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!
}