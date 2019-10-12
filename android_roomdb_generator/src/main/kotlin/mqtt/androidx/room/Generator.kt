package mqtt.androidx.room

import androidx.room.Database
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Type
import java.lang.reflect.InvocationTargetException
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic


@AutoService(Processor::class)
class MqttCodeGenerator : AbstractProcessor() {
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    private lateinit var filer: Filer
    private lateinit var messager: Messager
    private val dbClassRef = MqttDatabase::class.java
    private val roomDbClassRef = Database::class.java

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils!!
        elementUtils = processingEnv.elementUtils!!
        filer = processingEnv.filer!!
        messager = processingEnv.messager
    }

    private fun getEntitiesFromDatabaseAnnotation(mirror: AnnotationMirror): List<Attribute.Class>? {
        val compound = mirror.elementValues.values.filterIsInstance<Attribute.Compound>()
        compound.forEach { compound ->
            val result = getEntityFromCompound(compound)
            if (result != null && result.isNotEmpty()) {
                return result
            }
        }
        return null
    }

    private fun getEntityFromCompound(compound: Attribute.Compound): List<Attribute.Class>? {
        val elementValues = compound.elementValues as? LinkedHashMap<Symbol, Attribute> ?: return null
        val type = compound.type as Type.ClassType
        if (type.tsym.qualifiedName.toString() != Database::class.qualifiedName) {
            return null
        }
        val entitySymbol = elementValues.keys.firstOrNull { it.name.toString() == "entities" } ?: return null
        val elementsArray = elementValues[entitySymbol] as? Attribute.Array ?: return null
        return elementsArray.values.filterIsInstance<Attribute.Class>()
    }

    fun x(database: Database?) {
        database?.version
    }

    private fun TypeMirror.asTypeElement(): TypeElement {
        return typeUtils.asElement(this) as TypeElement
    }

    private fun <T : TypeMirror> List<T>.typeElements(): List<TypeElement> = map {
        it.asTypeElement()
    }

    private fun <T : TypeMirror> List<T>.classNames(): List<ClassName> = map {
        it.asTypeElement().asClassName()
    }


    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {

        roundEnv.getElementsAnnotatedWith(dbClassRef).forEach { annotatedElement ->
            val annotation = annotatedElement.getAnnotation(dbClassRef)!!
            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Annotation mirrors")
            val classAttributes = ArrayList<Attribute.Class>()
            annotatedElement.annotationMirrors.forEach { mirror ->
                messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, mirror.toString())
                val result = getEntitiesFromDatabaseAnnotation(mirror)
                if (result != null) {
                    classAttributes.addAll(result)
                }
            }

            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Annotation mirrors done")
            val roomDbAnnotation = annotation.db
            val pkg = elementUtils.getPackageOf(annotatedElement).qualifiedName!!.toString()
            val name = "Mqtt_RoomDb_${annotatedElement.simpleName}"


            val persistableRemoteHostV4ClassName =
                ClassName("mqtt.client.connection.parameters", "PersistableRemoteHostV4")
            val entities = ArrayList<ClassName>()
            entities += persistableRemoteHostV4ClassName
            try {
                AnnotationSpec.get(roomDbAnnotation)
                throw IllegalStateException("Should of thrown by now")
            } catch (e: RuntimeException) {
                val cause = e.cause as? InvocationTargetException
                val mirroredTypeException = cause?.targetException as? MirroredTypesException
                val elements = mirroredTypeException?.typeMirrors?.classNames()
                if (elements != null) {
                    entities.addAll(elements)
                }
            }

            val fileSpec = FileSpec.builder(pkg, name)
            val entitiesArrayString =
                entities.joinToString(prefix = "entities = [", postfix = "]", transform = { className ->
                    fileSpec.addImport(className.packageName, className.simpleName)
                    "${className.simpleName}::class"
                })

            val modifiedDbAnnotation = AnnotationSpec.builder(Database::class)
                .addMember(entitiesArrayString)
            fileSpec.addImport("androidx.room", "Database")
                .addImport("androidx.room", "Room")
                .addType(
                    TypeSpec.classBuilder(name)
                        .addModifiers(KModifier.ABSTRACT)
                        .addAnnotation(AnnotationSpec.builder(Database::class).addMember(entitiesArrayString).build())
                        .addType(
                            TypeSpec.Companion.companionObjectBuilder()
                                .addFunction(
                                    FunSpec.builder("getRoomDb")
                                        .addModifiers(KModifier.PUBLIC)
                                        .addParameter("context", ClassName("android.content", "Context"))
                                        .returns(ClassName(pkg, annotatedElement.simpleName!!.toString()))
                                        .addCode("return Room.databaseBuilder<${annotatedElement.simpleName}>(context, ${annotatedElement.simpleName}::class, \"mqtt.db\")")
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )

            val file = fileSpec.build()

            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, file.toString())
//            file.writeTo(File("/Users/thebehera/Desktop/test", "Yolo.kt"))
        }
        return true
    }

    override fun getSupportedAnnotationTypes() = setOf(MqttDatabase::class.qualifiedName!!)

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!
}