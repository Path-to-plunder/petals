package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.ClassName

class FunctionTemplate(name: String, receiver: ClassName, typeName: ClassName, function: () -> Unit) {
    fun addCode(format: String, vararg args: Any?) {
    }

    companion object {
        fun functionTemplate(name: String, receiver: ClassName, typeName: ClassName, function: FunctionTemplate.() -> Unit) {

        }
    }
}
