package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.CodeBlock

class CodeTemplate(internal val codeBlock: CodeBlock) {
    constructor(format: String, vararg args: Any?): this( CodeBlock.of(format, *args) )

    companion object {
        fun codeTemplate(function: () -> CodeBlock): CodeTemplate {
            return CodeTemplate(function())
        }
    }
}
