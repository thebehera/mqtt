package mqtt.androidx.room

import androidx.room.Database
import com.squareup.javapoet.*
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier
import javax.lang.model.util.Elements
import javax.lang.model.util.Types

class JavaAnnotatedMqttElement(
    processingEnv: ProcessingEnvironment,
    typeUtils: Types, elementUtils: Elements, element: Element, val annotation: MqttDatabase
) : AnnotatedMqttElement(processingEnv, typeUtils, elementUtils, element, annotation) {

    val classes = HashSet<ClassName>()

    override val entitiesString = entitiesInDatabaseAnnotation
        .joinToString(prefix = "{ ", postfix = " }", transform = {
            classes.add(ClassName.get(it.packageName, it.simpleName))
            "\$T.class"
        })

    override fun write(filer: Filer) {
        val generatedClass = ClassName.get(pkg, filename)
        val getRoomDbMethod = MethodSpec.methodBuilder("getRoomDb")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(ClassName.get("android.content", "Context"), "context")
            .returns(generatedClass)
            .addStatement(
                "return \$T.databaseBuilder(context, $filename.class, \"mqtt.db\").build()",
                ClassName.get("androidx.room", "Room")
            ).build()
        val classType = TypeSpec.classBuilder(filename)
            .superclass(ClassName.get(pkg, element.simpleName.toString()))
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .addAnnotation(
                AnnotationSpec.builder(Database::class.java)
                    .addMember("entities", entitiesString, *classes.toTypedArray())
                    .addMember("version", "${annotation.db.version}")
                    .build()
            )
            .addMethod(getRoomDbMethod)
            .build()

        val file = JavaFile.builder(pkg, classType).build()
        file.writeTo(filer)
    }
}