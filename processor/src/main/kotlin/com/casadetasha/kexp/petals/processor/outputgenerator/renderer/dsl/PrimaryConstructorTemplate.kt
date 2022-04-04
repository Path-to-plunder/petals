package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.FunSpec


class ConstructorTemplate(function: ConstructorTemplate.() -> Unit) {

    private val constructorBuilder = FunSpec.constructorBuilder()
    internal val constructorSpec: FunSpec

    init {
        this.function()
        constructorSpec = constructorBuilder.build()
    }

    internal fun addParameters(parameterTemplates: Collection<ParameterTemplate>) {
        constructorBuilder.addParameters(parameterTemplates.map{ it.parameterSpec })
    }

    internal fun addConstructorProperties(classTemplate: ClassTemplate, properties: Collection<ConstructorPropertyTemplate>) {
        addParameters(properties.map { ParameterTemplate(name = it.name, typeName = it.typeName) })
        classTemplate.addProperties(properties)
    }

    companion object {
        fun ClassTemplate.primaryConstructorTemplate(function: ConstructorTemplate.() -> Unit) {
            addPrimaryConstructor(ConstructorTemplate(function))
        }
    }
}
