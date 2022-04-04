package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

interface KotlinContainerTemplate {

    fun addFunction(functionTemplate: FunctionTemplate)
    fun addProperties(properties: Collection<PropertyTemplate>)
}