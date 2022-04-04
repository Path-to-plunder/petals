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
        writeCode(CodeTemplate(format, args))
    }

    fun writeCode(codeTemplate: CodeTemplate) {
        functionBuilder.addCode(codeTemplate.codeBlock)
    }

    fun collectCode(function: () -> List<CodeTemplate>) {
        function().forEach {
            functionBuilder.addCode(it.codeBlock)
        }
    }

    fun parenthesisedBlock(startingString: String, function: FunctionTemplate.() -> Unit) {
        functionBuilder.addCode("$startingString(")
        this.function()
        functionBuilder.addCode("\n)")
    }

    companion object {
        fun ClassTemplate.functionTemplate(
            name: String,
            receiverType: ClassName?,
            returnType: ClassName?,
            function: FunctionTemplate.() -> Unit
        ) {
            addFunction(FunctionTemplate(name, receiverType, returnType, function))
        }
    }
}
