package mqtt.androidx.room

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.*
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
    private val publishQueueRef = MqttPublishQueue::class
    private val serializerRef = MqttSerializer::class

    val kaptKotlinGeneratedDir by lazy {
        File(processingEnv.options["kapt.kotlin.generated"] ?: run {
            val msg = "Can't find the target directory for generated Kotlin files."
            messager.printMessage(Diagnostic.Kind.ERROR, msg)
            throw RuntimeException(msg)
        })
    }
    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils!!
        elementUtils = processingEnv.elementUtils!!
        filer = processingEnv.filer!!
        messager = processingEnv.messager
    }


    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        return try {
            process2(roundEnv)
        } catch (e: Exception) {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            messager.printMessage(Diagnostic.Kind.ERROR, "\nFATAL EXCEPTION in AbstractProcessor ($this)\n$sw")
            throw e
        }
    }

    private fun process2(roundEnv: RoundEnvironment): Boolean {
        val dbMappingToClassName = HashMap<String, ClassName>()
        val database = roundEnv.getElementsAnnotatedWith(dbClassRef.java).firstOrNull() as? TypeElement ?: return true
        val dbAnnotation = database.getAnnotation(dbClassRef.java)
        val mqttElement =
            JavaAnnotatedMqttElement(processingEnv, typeUtils, elementUtils, database, dbAnnotation)
        try {
            dbAnnotation.db.entities
        } catch (e: MirroredTypesException) {
            e.typeMirrors.classNames().forEach { className ->
                dbMappingToClassName[className.canonicalName] = mqttElement.kClassName
            }
        }

        val publishModels = roundEnv.getElementsAnnotatedWith(publishRef.java)
            .filter {
                it.kind == ElementKind.CLASS
            }.filterIsInstance<TypeElement>().associateWith {
                val annotation = it.getAnnotation(publishRef.java)
                annotation
            }
        val classNameToPublishMap = publishModels.map {
            it.key.asClassName() to it.value
        }.toMap()
        val publishFullNameMap = publishModels.keys.associateBy { it.asType().asTypeName().toString() }

        val mqttQueueAnnotatedElements = roundEnv.getElementsAnnotatedWith(publishQueueRef.java)
            .filter { it.kind == ElementKind.METHOD }
            .filterIsInstance<ExecutableElement>()
            .map {
                val model = it.parameters[0] as VariableElement
                val declaredType = model.asType() as DeclaredType
                val typeElement = declaredType.asElement() as TypeElement
                typeElement.asClassName() to PublishQueueParams(
                    it,
                    publishModels[typeElement]!!,
                    it.getAnnotation(publishQueueRef.java),
                    database
                )
            }.toMap()

        val annotatedTypeToSerializationType = HashMap<TypeElement, TypeElement>()
        val typeToSerializer = roundEnv.getElementsAnnotatedWith(serializerRef.java)
            .filterIsInstance<TypeElement>()
            .filter { it.kind == ElementKind.CLASS }.associate { annotatedTypeElement ->
                val annotation = annotatedTypeElement.getAnnotation(serializerRef.java)
                val serializedDeclaredType = annotatedTypeElement.interfaces.asSequence()
                    .filterIsInstance<DeclaredType>()
                    .filter {
                        it.asElement().qualifiedName(elementUtils) == "mqtt.wire.control.packet.MqttSerializable"
                    }.first().typeArguments.first() as DeclaredType
                val serializerTypeElement = serializedDeclaredType.asElement() as TypeElement
                annotatedTypeToSerializationType[serializerTypeElement] = annotatedTypeElement
                serializerTypeElement to annotation
            }


        val annotatedTypeToPublishDequeMethodType = HashMap<TypeElement, ExecutableElement>()
        roundEnv.getElementsAnnotatedWith(publishDequeRef.java)
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
                            .first { typeName ->
                                val typeElement = publishFullNameMap[typeName]
                                if (typeElement != null) {
                                    annotatedTypeToPublishDequeMethodType[typeElement] = executableElement
                                }
                                typeElement != null
                            }

                        publishFullNameMap.getValue(typeName)
                    }

                val firstTypeElement = parameters.first()
                val annotation = executableElement.getAnnotation(publishDequeRef.java)!!
                firstTypeElement to annotation
            }


        val serializers = annotatedTypeToSerializationType.values.map { it.asClassName() }
        val classNameToPublishAnnotations = publishModels.asSequence().associate {
            ClassName(elementUtils.getPackageOf(it.key).toString(), it.key.simpleName.toString()) to it.value
        }
        val mqttDbProvierFileSpec = fileSpec(mqttElement.kClassName, serializers, classNameToPublishAnnotations)
        mqttDbProvierFileSpec.writeTo(kaptKotlinGeneratedDir)
        messager.printMessage(Diagnostic.Kind.NOTE, "\nWrote \n $mqttDbProvierFileSpec\n")


        val viewModelFileSpec = fileSpec(
            mqttElement.pkg,
            classNameToPublishMap,
            mqttQueueAnnotatedElements
        )
        viewModelFileSpec.writeTo(kaptKotlinGeneratedDir)
        messager.printMessage(Diagnostic.Kind.NOTE, "\nWrote \n $viewModelFileSpec\n")

        publishModels.forEach {
            val packetFound = typeToSerializer[it.key]
            if (packetFound == null) {
                messager.printMessage(Diagnostic.Kind.WARNING, "Failed to find @MqttPublishPacket for $it", it.key)
            } else {
                val publishPacket = typeToSerializer[it.key]
                val publishPacketElement = annotatedTypeToSerializationType[it.key]
                val dequeueFound = annotatedTypeToPublishDequeMethodType[it.key]

                if (dequeueFound == null) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Failed to find @MqttPublishDequeue for $it", it.key)
                }
                val type = it.key.asType()
                if (type != null) {

                    val roomDbClassName = dbMappingToClassName[it.key.asClassName().canonicalName]!!
                    val serializationClass = annotatedTypeToSerializationType[it.key]
                    if (serializationClass != null && publishPacket != null && publishPacketElement != null) {

                        //TODO: Update so we can pass in the correct ExecutableElement(s)

                        val generated =
//                            GeneratedRoomQueuedObjectCollectionGenerator(
//                            elementUtils, it.key, roomDbClassName, it.value,
//                            publishPacket, publishPacketElement,
//                            publishDequeueReturnTypes[it.key], annotatedTypeToPublishDequeMethodType[it.key])
                            GeneratedRoomQueuedObjectCollectionGenerator(
                                elementUtils,
                                it.key,
                                roomDbClassName,
                                serializationClass,
                                dequeueFound
                            )

                        generated.classSpecPersist.writeTo(kaptKotlinGeneratedDir)
                        messager.printMessage(Diagnostic.Kind.NOTE, "\nWrote \n ${generated.classSpecPersist}\n")
                    }
                }
            }
        }
        mqttElement.write(filer)
        return true
    }

    override fun getSupportedAnnotationTypes() =
        setOf(
            MqttDatabase::class.qualifiedName!!,
            MqttPublish::class.qualifiedName!!,
            MqttPublishQueue::class.qualifiedName!!,
            MqttPublishDequeue::class.qualifiedName!!,
            MqttSerializer::class.qualifiedName!!
        )

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!


    private fun <T : TypeMirror> List<T>.classNames() = map { (typeUtils.asElement(it) as TypeElement).asClassName() }
}

fun Element.qualifiedName(elementUtils: Elements): String {
    return "${elementUtils.getPackageOf(this).qualifiedName}.$simpleName"
}