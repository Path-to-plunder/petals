package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.CodeBlock

class CodeTemplate(format: String, vararg args: Any?) {
    internal val codeBlock: CodeBlock
    init {
        codeBlock = CodeBlock.of(format, *args)
    }
}
