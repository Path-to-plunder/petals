package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.KotlinTemplate.toKModifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName

class FunctionTemplate(name: String,
                       receiverType: TypeName? = null,
                       returnType: TypeName? = null,
                       function: FunctionTemplate.() -> Unit
) {

    private val functionBuilder = FunSpec.builder(name)
    internal val functionSpec: FunSpec

    init {
        receiverType?.let { functionBuilder.receiver(receiverType) }
        returnType?.let { functionBuilder.returns(returnType) }

        this.function()
        functionSpec = functionBuilder.build()
    }

    fun writeCode(format: String, vararg args: Any?) {
        functionBuilder.addCode(format, *args)
    }

    fun writeCode(function: () -> CodeTemplate) {
        functionBuilder.addCode(function().codeBlock)
    }

    fun collectCode(function: () -> Collection<CodeTemplate>) {
        function().forEach {
            functionBuilder.addCode(it.codeBlock)
        }
    }

    fun parenthesizedBlock(startingString: String, function: FunctionTemplate.() -> Unit) {
        functionBuilder.addCode("$startingString(")
        this.function()
        functionBuilder.addCode("\n)")
    }

    fun override() {
        functionBuilder.addModifiers(KModifier.OVERRIDE)
    }

    fun visibility(function: () -> KotlinTemplate.Visibility) {
        functionBuilder.addModifiers(function().toKModifier())
    }

    internal fun addParameter(parameterTemplate: ParameterTemplate) {
        functionBuilder.addParameter(parameterTemplate.parameterSpec)
    }

    internal fun addParameters(parameterTemplates: Collection<ParameterTemplate>) {
        functionBuilder.addParameters(parameterTemplates.map{ it.parameterSpec })
    }

    companion object {

        fun KotlinContainerTemplate.collectFunctionTemplates(function: KotlinContainerTemplate.() -> Collection<FunctionTemplate>) {
            function().forEach { addFunction(it) }
        }

        fun KotlinContainerTemplate.functionTemplate(
            name: String,
            receiverType: ClassName? = null,
            returnType: ClassName? = null,
            function: FunctionTemplate.() -> Unit
        ) {
            addFunction(FunctionTemplate(name, receiverType, returnType, function))
        }
    }
}
