package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.casadetasha.kexp.annotationparser.AnnotationParser.getClassesAnnotatedWith
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.VarChar
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassFileGenerator
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.classgenerator.table.TableGenerator
import com.casadetasha.kexp.petals.processor.migration.MigrationGenerator
import com.casadetasha.kexp.petals.processor.migration.PetalMigrationSetupGenerator
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
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
            val className = petalAnnotation.className
            val tableVersion = petalAnnotation.version
            if (tableMap[tableName] == null) tableMap[tableName] = PetalMigration(
                tableName = tableName,
                className = className
            )
            tableMap[tableName]!!.schemaMigrations[tableVersion] = PetalSchemaMigrationParser.parseFromClass(it)
        }

        tableMap.values.forEach { migration ->
            MigrationGenerator(migration).createMigrationForTable()
            migration.getCurrentSchema()?.let {
                TableGenerator(migration.className, migration.tableName, it).generateFile()
                AccessorClassFileGenerator(migration.getAccessorClassInfo()).generateFile()
            }
        }

        if (tableMap.isNotEmpty()) {
            PetalMigrationSetupGenerator(tableMap.values).createPetalMigrationSetupClass()
        }
    }
}

private fun PetalMigration.getAccessorClassInfo(): AccessorClassInfo {
    return AccessorClassInfo(
        packageName = "com.casadetasha.kexp.petals.accessor",
        simpleName = className,
        sourceClassName = ClassName("com.casadetasha.kexp.petals", "${className}Entity"),
        columns = getCurrentSchema()!!.columnsAsList.toSet()
    )
}
