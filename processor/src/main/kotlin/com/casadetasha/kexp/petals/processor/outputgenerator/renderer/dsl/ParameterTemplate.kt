package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.addDefaultValue
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.KotlinTemplate.toKModifier
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

class ParameterTemplate internal constructor(val name: String,
                                             val typeName: TypeName,
                                             function: (ParameterTemplate.() -> Unit)? = null) {

    private var _defaultValue: CodeTemplate? = null

    internal val parameterSpec: ParameterSpec by lazy {
        val paramBuilder = ParameterSpec.builder(name, typeName)

        function?.let { this.function() }

        _defaultValue?.let { paramBuilder.defaultValue( _defaultValue!!.codeBlock ) }

        paramBuilder.build()
    }

    fun defaultValue(function: () -> CodeTemplate) {
        _defaultValue = function()
    }

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
