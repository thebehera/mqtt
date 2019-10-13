package mqtt.androidx.room

import androidx.room.Database
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

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        typeUtils = processingEnv.typeUtils!!
        elementUtils = processingEnv.elementUtils!!
        filer = processingEnv.filer!!
        messager = processingEnv.messager
    }

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val databases = roundEnv.getElementsAnnotatedWith(dbClassRef).map {
            JavaAnnotatedMqttElement(processingEnv, typeUtils, elementUtils, it, it.getAnnotation(dbClassRef)!!)
        }
        databases.forEach {
            it.write(filer)
        }
        return true
    }

    override fun getSupportedAnnotationTypes() =
        setOf(Database::class.qualifiedName, MqttDatabase::class.qualifiedName!!)

    override fun getSupportedSourceVersion() = SourceVersion.latestSupported()!!
}