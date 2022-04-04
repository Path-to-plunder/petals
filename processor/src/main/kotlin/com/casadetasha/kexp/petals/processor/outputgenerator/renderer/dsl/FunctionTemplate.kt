package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec

class FunctionTemplate(name: String, receiverType: ClassName?, returnType: ClassName?, function: FunctionTemplate.() -> Unit) {

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

    fun collectCode(function: () -> List<CodeTemplate>) {
        function().forEach {
            functionBuilder.addCode(it.codeBlock)
        }
    }

    fun parenthesizedBlock(startingString: String, function: FunctionTemplate.() -> Unit) {
        functionBuilder.addCode("$startingString(")
        this.function()
        functionBuilder.addCode("\n)")
    }

    companion object {

        fun KotlinContainerTemplate.functionTemplate(
            name: String,
            receiverType: ClassName?,
            returnType: ClassName?,
            function: FunctionTemplate.() -> Unit
        ) {
            addFunction(FunctionTemplate(name, receiverType, returnType, function))
        }
    }
}
