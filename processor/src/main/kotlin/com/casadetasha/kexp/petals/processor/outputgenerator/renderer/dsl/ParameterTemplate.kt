package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

class ParameterTemplate internal constructor(val name: String, val typeName: TypeName) {

    val parameterSpec: ParameterSpec = ParameterSpec.builder(name, typeName).build()

    companion object {
        fun ConstructorTemplate.collectParameterTemplates(function: () -> Collection<ParameterTemplate>) {
            addParameters(function())
        }

        fun ConstructorTemplate.parameterTemplate(name: String, typeName: TypeName) {
            addParameters(
                listOf(
                    ParameterTemplate(
                        name = name,
                        typeName = typeName
                    )
                )
            )
        }
    }
}
