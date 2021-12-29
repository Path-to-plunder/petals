package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.casadetasha.kexp.annotationparser.AnnotationParser.getClassesAnnotatedWith
import com.casadetasha.kexp.annotationparser.kxt.getClassName
import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.OtherColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.Name
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class PetalProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() = mutableSetOf(
        Petal::class.java.canonicalName,
        AlterColumn::class.java.canonicalName
    )

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        try {
            AnnotationParser.setup(processingEnv, roundEnv)
        } catch (_: IllegalStateException) {
            return false
        }

        generateClasses()
        return true
    }

    private fun generateClasses() {
        val classes = getClassesAnnotatedWith(Petal::class,
            propertyAnnotations = listOf(AlterColumn::class))

        val tableMap = HashMap<String, MutableMap<Int, PetalMigration>>()

        classes.forEach {
            val petalAnnotation: Petal = it.getAnnotation(Petal::class)!!
            val table = petalAnnotation.tableName
            if (tableMap[table] == null) tableMap[table] = HashMap()
            tableMap[table]!![petalAnnotation.version] = PetalMigration.parseFromClass(it)
        }

        tableMap.values.forEach {
            MigrationGenerator().createMigrationForTable(it)
        }
    }
}
