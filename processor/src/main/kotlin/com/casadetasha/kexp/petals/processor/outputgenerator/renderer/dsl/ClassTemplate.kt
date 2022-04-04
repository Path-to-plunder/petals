package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class ClassTemplate private constructor(className: ClassName,
                    private val modifiers: Collection<KModifier>?,
                    annotations: Collection<AnnotationTemplate>?,
                    function: ClassTemplate.() -> Unit): KotlinContainerTemplate {

    private val classBuilder = TypeSpec.classBuilder(className)

    internal val classSpec: TypeSpec

    init {
        annotations?.let {
            classBuilder.addAnnotations(
                it.map { annotationTemplate -> annotationTemplate.annotationSpec }
            )
        }

        modifiers?.let {
            classBuilder.addModifiers(modifiers)
        }

        this.function()

        classSpec = classBuilder.build()
    }

    override fun addFunction(functionTemplate: FunctionTemplate) {
        classBuilder.addFunction(functionTemplate.functionSpec)
    }

    override fun addProperties(properties: Collection<PropertyTemplate>) {
        classBuilder.addProperties(properties.map { it.propertySpec } )
    }

    fun addPrimaryConstructor(primaryConstructorTemplate: ConstructorTemplate) {
        classBuilder.primaryConstructor(primaryConstructorTemplate.constructorSpec)
    }

    companion object {

        fun FileTemplate.classTemplate(
            name: ClassName,
            modifiers: Collection<KModifier>?,
            annotations: Collection<AnnotationTemplate>? = null,
            function: ClassTemplate.() -> Unit
        ) {
            addClass(ClassTemplate(name, modifiers, annotations, function))
        }
    }
}

