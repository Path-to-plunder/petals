package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.casadetasha.kexp.annotationparser.AnnotationParser.getClassesAnnotatedWith
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.VarChar
import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class PetalProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() = mutableSetOf(
        Petal::class.java.canonicalName,
        AlterColumn::class.java.canonicalName,
        VarChar::class.java.canonicalName
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
            propertyAnnotations = listOf(AlterColumn::class, VarChar::class))

        val tableMap = HashMap<String, PetalMigration>()

        classes.forEach {
            val petalAnnotation: Petal = it.getAnnotation(Petal::class)!!
            val tableName = petalAnnotation.tableName
            val tableVersion = petalAnnotation.version
            if (tableMap[tableName] == null) tableMap[tableName] = PetalMigration(tableName)
            tableMap[tableName]!!.schemaMigrations[tableVersion] = PetalSchemaMigrationParser.parseFromClass(it)
        }

        tableMap.values.forEach { migration ->
            MigrationGenerator(migration).createMigrationForTable()
            migration.getCurrentSchema()?.let { DaoGenerator(migration.tableName, it).generateFile() }
        }

        if (tableMap.isNotEmpty()) {
            PetalMigrationSetupGenerator(tableMap.values).createPetalMigrationSetupClass()
        }
    }
}
