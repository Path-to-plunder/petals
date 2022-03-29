package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.*
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.MigrationGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.PetalMigrationSetupGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.PetalSchemaMigrationParser
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalMigration
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DataClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedClassesFileGenerator
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException

@AutoService(Processor::class)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class PetalProcessor : AbstractProcessor() {

    private val petalClasses = PetalClasses()

    override fun getSupportedAnnotationTypes() : MutableSet<String> {
        val supportedAnnotationTypes = setOf(
            Petal::class.java.canonicalName,
            PetalSchema::class.java.canonicalName
        ) + petalClasses.SUPPORTED_PROPERTY_ANNOTATIONS.map { it.java.canonicalName }

        return supportedAnnotationTypes.toMutableSet()
    }

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
        val tableMap = UnprocessedPetalMigrationMap()

        petalClasses.SCHEMA_CLASSES.forEach {
            tableMap.insertMigrationForPetalSchema(it)
        }

        petalClasses.RUNTIME_SCHEMAS = tableMap.values
            .map { it.schemaMigrations.values.last() }
            .associateBy { it.petalClass }

        tableMap.values.forEach { migration ->
            MigrationGenerator(migration).createMigrationForTable()
            migration.getCurrentSchema()?.let {
                val accessorClassInfo = migration.getAccessorClassInfo()
                ExposedClassesFileGenerator(petalClasses, migration.className, migration.tableName, it).generateFile()
                com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassFileGenerator(
                    accessorClassInfo
                ).generateFile()
                DataClassFileGenerator(accessorClassInfo).generateFile()
            }
        }

        if (tableMap.isNotEmpty()) {
            PetalMigrationSetupGenerator(tableMap.values).createPetalMigrationSetupClass()
        }
    }

    private fun UnprocessedPetalMigrationMap.insertMigrationForPetalSchema(it: KotlinContainer.KotlinClass) {
        val petalSchemaAnnotation: PetalSchema = it.getAnnotation(PetalSchema::class)!!
        val petalClassName = petalSchemaAnnotation.petalTypeName
        val petal: KotlinContainer.KotlinClass =
            checkNotNull(petalClasses.PETAL_CLASSES[petalClassName]) { "Parameter \"petal\" for PetalSchema must be a Petal annotated class." }
        val petalAnnotation =
            checkNotNull(petal.getAnnotation(Petal::class)) { "Parameter \"petal\" for PetalSchema must be a Petal annotated class." }
        val tableName = petalAnnotation.tableName
        val className = petalAnnotation.className
        val tableVersion = petalSchemaAnnotation.version
        if (this[tableName] == null) this[tableName] = UnprocessedPetalMigration(
            tableName = tableName,
            className = className
        )

        this[tableName]!!.schemaMigrations[tableVersion] = PetalSchemaMigrationParser(petalClasses)
            .parseFromClass(petalClassName, it, petalAnnotation.primaryKeyType)
    }
}

private typealias UnprocessedPetalMigrationMap = HashMap<String, UnprocessedPetalMigration>

private fun UnprocessedPetalMigration.getAccessorClassInfo(): com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo {
    return com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo(
        packageName = "com.casadetasha.kexp.petals.accessor",
        simpleName = className,
        entityClassName = ClassName("com.casadetasha.kexp.petals", "${className}Entity"),
        tableClassName = ClassName("com.casadetasha.kexp.petals", "${className}Table"),
        dataClassName = ClassName("com.casadetasha.kexp.petals.data", "${className}Data"),
        columns = getCurrentSchema()!!.columnsAsList.toSet()
    )
}

// asTypeName() should be safe since custom routes will never be Kotlin core classes
@OptIn(DelicateKotlinPoetApi::class)
private val PetalSchema.petalTypeName: TypeName
    get() = try {
            ClassName(petal.java.packageName, petal.java.simpleName)
        } catch (exception: MirroredTypeException) {
            exception.typeMirror.asTypeName()
        }
