package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.casadetasha.kexp.petals.annotations.*
import com.casadetasha.kexp.petals.processor.inputparser.PetalAnnotationParser
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.outputgenerator.PetalFileGenerator
import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class PetalProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() : MutableSet<String> {
        val supportedAnnotationTypes = setOf(
            Petal::class.java.canonicalName,
        ) + PetalClasses.SUPPORTED_SCHEMA_ANNOTATIONS.map {
            it.java.canonicalName
        } + PetalClasses.SUPPORTED_PROPERTY_ANNOTATIONS.map {
            it.java.canonicalName
        }

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
        val petalClasses: PetalClasses = PetalAnnotationParser.parsePetalClasses()

        PetalFileGenerator(petalClasses).generateFiles()
    }
}
