package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

class ParameterTemplate(name: String, typeName: TypeName) {

    val parameterSpec: ParameterSpec

    init {
        parameterSpec = ParameterSpec.builder(name, typeName).build()
    }

    companion object {
        fun ConstructorTemplate.parameterTemplate(name: String, typeName: TypeName) {
            addParameter(ParameterTemplate(name, typeName))
        }

        fun ConstructorTemplate.collectParameters(function: () -> Collection<ParameterTemplate>) {
            addParameters(function())
        }
    }
}
