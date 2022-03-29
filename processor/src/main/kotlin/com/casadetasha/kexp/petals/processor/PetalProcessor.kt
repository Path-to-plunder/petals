package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.*
import com.casadetasha.kexp.petals.processor.inputparser.PetalAnnotationParser
import com.casadetasha.kexp.petals.processor.inputparser.UnprocessedPetalMigrationMap
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.MigrationGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.PetalMigrationSetupGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.PetalSchemaMigrationParser
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalMigration
import com.casadetasha.kexp.petals.processor.outputgenerator.PetalFileGenerator
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
        val petalAnnotationParser = PetalAnnotationParser(petalClasses)
        petalAnnotationParser.parsePetalMap()

        PetalFileGenerator(petalClasses, petalAnnotationParser.tableMap).generateFiles()
    }
}
