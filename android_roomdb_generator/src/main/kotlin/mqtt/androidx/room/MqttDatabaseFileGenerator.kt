package mqtt.androidx.room

import androidx.room.Database
import com.squareup.kotlinpoet.*
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.*
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypesException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

open class AnnotatedMqttElement(
    val processingEnv: ProcessingEnvironment,
    private val typeUtils: Types,
    val elementUtils: Elements,
    val element: Element, private val annotation: MqttDatabase
) {
    val pkg = elementUtils.getPackageOf(element).qualifiedName!!.toString()
    val filename = "Mqtt_RoomDb_${element.simpleName}"
    val kClassName = ClassName(pkg, filename)

    val entitiesInDatabaseAnnotation by lazy {
        val _entities = TreeSet<ClassName>()
        try {
            AnnotationSpec.get(annotation.db)
            throw IllegalStateException("Should of thrown by now")
        } catch (e: RuntimeException) {
            val cause = e.cause as? InvocationTargetException
            val mirroredTypeException = cause?.targetException as? MirroredTypesException
            val elements = mirroredTypeException?.typeMirrors?.classNames()
            if (elements != null) {
                _entities.addAll(elements)
            }
        }
        _entities += arrayOf(
            persistableRemoteHostV4ClassName,
            persistedQueueV4ClassName,
            publishQueueV4ClassName,
            subscriptionClassName
        )
        _entities
    }


    open val entitiesString = entitiesInDatabaseAnnotation
        .joinToString(prefix = "entities = [", postfix = "]", transform = { "${it.simpleName}::class" })

    private val file = File(processingEnv.options["kapt.kotlin.generated"], "Yolo.kt")

    open fun write(filer: Filer) {
        val fileSpec = FileSpec.builder(pkg, filename).also {
            it.addImport("androidx.room", "Database", "Room")
            it.addImport(elementUtils.getPackageOf(element).toString(), element.simpleName.toString())
            entitiesInDatabaseAnnotation.forEach { entity -> it.addImport(entity.packageName, entity.simpleName) }
            it.addType(
                TypeSpec.classBuilder(filename)
                    .addModifiers(KModifier.ABSTRACT)
                    .addAnnotation(
                        AnnotationSpec.builder(Database::class)
                            .addMember(entitiesString)
                            .addMember("version = ${annotation.db.version}")
                            .build()
                    )
                    .superclass(element.asType().asTypeName())
                    .addType(
                        TypeSpec.Companion.companionObjectBuilder()
                            .addFunction(
                                FunSpec.builder("getRoomDb")
                                    .addModifiers(KModifier.PUBLIC)
                                    .addParameter("context", ClassName("android.content", "Context"))
                                    .returns(ClassName(pkg, filename))
                                    .addStatement("return Room.databaseBuilder<$filename>(context, $filename::class.java, \"mqtt.db\").build()")

                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
        }.build()
        fileSpec.writeTo(filer)
    }


    private fun <T : TypeMirror> List<T>.classNames() = map { (typeUtils.asElement(it) as TypeElement).asClassName() }

    companion object {

        val persistableRemoteHostV4ClassName =
            ClassName("mqtt.client.persistence", "PersistableRemoteHostV4")
        val persistedQueueV4ClassName =
            ClassName("mqtt.client.persistence", "MqttQueue")
        val publishQueueV4ClassName =
            ClassName("mqtt.client.persistence", "MqttPublishQueue")
        val subscriptionClassName =
            ClassName("mqtt.client.persistence", "MqttSubscription")
    }
}

