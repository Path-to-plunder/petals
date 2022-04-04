package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.AnnotationSpec
import kotlin.reflect.KClass

class AnnotationTemplate(private val kClass: KClass<out Annotation>) {

    private val annotationBuilder = AnnotationSpec.builder(kClass)
    internal val annotationSpec: AnnotationSpec by lazy {
        annotationBuilder.build()
    }

    constructor(kClass: KClass<out Annotation>, function: AnnotationTemplate.() -> Unit): this(kClass) {
        this.function()
    }

    fun addMember(format: String, vararg args: Any) {
        annotationBuilder.addMember(format, *args)
    }
}
