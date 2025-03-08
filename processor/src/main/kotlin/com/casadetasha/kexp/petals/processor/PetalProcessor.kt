package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.KAPT_KOTLIN_GENERATED_OPTION_NAME
import com.casadetasha.kexp.petals.annotations.*
import com.casadetasha.kexp.petals.processor.inputparser.PetalAnnotationParser
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.model.PetalClasses.Companion.SUPPORTED_PROPERTY_ANNOTATIONS
import com.casadetasha.kexp.petals.processor.model.PetalClasses.Companion.SUPPORTED_SCHEMA_ANNOTATIONS
import com.casadetasha.kexp.petals.processor.outputgenerator.PetalFileGenerator
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.MemberName
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import kotlin.reflect.KClass

@AutoService(Processor::class)
@SupportedOptions(KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class PetalProcessor : AbstractProcessor() {

    override fun getSupportedAnnotationTypes() : MutableSet<String> {
        val petalAnnotations = listOf(Petal::class.java.canonicalName)
        val schemaAnnotations = SUPPORTED_SCHEMA_ANNOTATIONS.map { it.java.canonicalName }
        val propertyAnnotations = SUPPORTED_PROPERTY_ANNOTATIONS.map { it.java.canonicalName }

        return setOf(petalAnnotations + schemaAnnotations + propertyAnnotations).flatten().toMutableSet()
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
