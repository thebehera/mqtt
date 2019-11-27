package mqtt.androidx.room

import androidx.room.Database
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.WildcardType
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic

@AutoService(Processor::class)
class MqttCodeGenerator : AbstractProcessor() {
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private val dbClassRef = MqttDatabase::class
    private val publishRef = MqttPublish::class
    private val publishDequeRef = MqttPublishDequeue::class
    private val publishPacketRef = MqttPublishPacket::class

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils!!
        elementUtils = processingEnv.elementUtils!!
        filer = processingEnv.filer!!
        messager = processingEnv.messager
    }


    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return try {
            process2(annotations, roundEnv)
        } catch (e: Exception) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            messager.printMessage(Diagnostic.Kind.ERROR, "\nFATAL EXCEPTION in AbstractProcessor ($this)\n$sw")
            true
        }
    }

    private fun process2(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val dbMappingToClassName = HashMap<String, ClassName>()
        val databases = roundEnv.getElementsAnnotatedWith(dbClassRef.java).map {
            val annotation = it.getAnnotation(dbClassRef.java)
            val mqttElement =
                JavaAnnotatedMqttElement(processingEnv, typeUtils, elementUtils, it, annotation)
            try {
                annotation.db.entities
            } catch (e: MirroredTypesException) {
                e.typeMirrors.classNames().forEach { className ->
                    dbMappingToClassName[className.canonicalName] = mqttElement.kClassName
                }
            }
            mqttElement
        }

        val publishModels = roundEnv.getElementsAnnotatedWith(publishRef.java)
            .filter {
                it.kind == ElementKind.CLASS
            }.associate {
                val annotation = it.getAnnotation(publishRef.java)
                it as TypeElement to annotation
            }
        val publishFullNameMap = publishModels.keys.associateBy { it.asType().asTypeName().toString() }

        val publishPacketsReturnTypes = roundEnv.getElementsAnnotatedWith(publishPacketRef.java)
            .filter { it.kind == ElementKind.METHOD }
            .associate {
                val annotation = it.getAnnotation(publishPacketRef.java)!!
                it.enclosingElement!! as TypeElement to annotation
            }

        val publishDequeueReturnTypes = roundEnv.getElementsAnnotatedWith(publishDequeRef.java)
            .filter { it.kind == ElementKind.METHOD }
            .filterIsInstance<ExecutableElement>()
            .associate { executableElement ->
                val parameters = executableElement.parameters
                    .filterIsInstance<Element>()
                    .map { parameterElement -> parameterElement.asType() }
                    .filterIsInstance<DeclaredType>()
                    .map { declaredType ->
                        val typeName = declaredType.typeArguments.filterIsInstance<WildcardType>()
                            .map { wildcardType -> wildcardType.superBound.asTypeName().toString() }
                            .first { typeName -> publishFullNameMap.containsKey(typeName) }
                        publishFullNameMap.getValue(typeName)
                    }

                val firstTypeElement = parameters.first()
                val annotation = executableElement.getAnnotation(publishDequeRef.java)!!
                firstTypeElement to annotation
            }

        val annotatedObjects = HashMap<TypeMirror, AnnotatedObjects>(publishModels.size)
        publishModels.forEach {
            val packetFound = publishPacketsReturnTypes[it.key]
            if (packetFound == null) {
                messager.printMessage(Diagnostic.Kind.WARNING, "Failed to find @MqttPublishPacket for $it")
            } else {
                val dequeueFound = publishDequeueReturnTypes[it.key]
                val publishPacket = publishPacketsReturnTypes[it.key]
                if (dequeueFound == null) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Failed to find @MqttPublishDequeue for $it")
                }
                val type = it.key.asType()
                if (type != null) {
                    annotatedObjects[type] = AnnotatedObjects(it.key, it.value, packetFound, dequeueFound)

                    val roomDbClassName = dbMappingToClassName[it.key.asClassName().canonicalName]!!

                    if (publishPacket != null) {
                        val generated = GeneratedRoomQueuedObjectCollectionGenerator(
                            elementUtils, it.key, roomDbClassName, it.value,
                            publishPacket, publishDequeueReturnTypes[it.key]
                        )
                        val spec = generated.classSpec.toString()

                        messager.printMessage(Diagnostic.Kind.NOTE, spec)
                    }
                }
            }
        }

        databases.forEach { it.write(filer) }
        val dbString = databases.toString()
        return true
    }

    override fun getSupportedAnnotationTypes() =
        setOf(
            Database::class.qualifiedName!!,
            MqttDatabase::class.qualifiedName!!,
            MqttPublish::class.qualifiedName!!,
            MqttPublishDequeue::class.qualifiedName!!,
            MqttPublishPacket::class.qualifiedName!!
        )

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!


    private fun <T : TypeMirror> List<T>.classNames() = map { (typeUtils.asElement(it) as TypeElement).asClassName() }
}


fun Element.qualifiedName(elementUtils: Elements): String {
    return "${elementUtils.getPackageOf(this).qualifiedName}.$simpleName"
}


data class AnnotatedObjects(
    val model: TypeElement,
    val publishAnnotation: MqttPublish,
    val packetAnnotation: MqttPublishPacket,
    val publishDequeueAnnotation: MqttPublishDequeue?
)