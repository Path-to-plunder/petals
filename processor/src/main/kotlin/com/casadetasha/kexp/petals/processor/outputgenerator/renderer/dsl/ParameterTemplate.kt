package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

class ParameterTemplate(val name: String, val typeName: TypeName) {

    val parameterSpec: ParameterSpec = ParameterSpec.builder(name, typeName).build()

    companion object {
        fun ConstructorTemplate.collectParameters(function: () -> Collection<ParameterTemplate>) {
            addParameters(function())
        }
    }
}
