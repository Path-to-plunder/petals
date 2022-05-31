package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.CodeBlock

class CodeTemplate(startingCodeBlock: CodeBlock? = null, function: (CodeTemplate.() -> Unit)? = null) {

    private val builder: CodeBlock.Builder = CodeBlock.builder()
    internal val codeBlock: CodeBlock

    constructor(format: String,
                vararg args: Any?,
                function: (CodeTemplate.() -> Unit)? = null
    ): this(CodeBlock.of(format, *args), function)

    init {
        startingCodeBlock?.let { builder.add(startingCodeBlock) }
        function?.let { this.function() }
        codeBlock = builder.build()
    }

    fun controlBlock(prefix: String, suffix: String = "", function: (CodeTemplate.() -> Unit)? = null) {
        builder.add("$prefix·{\n")
        builder.indent()

        function?.let { this.function() }

        builder.unindent()
        builder.add("}$suffix\n")
    }

    fun collectCodeTemplates(function: () -> Collection<CodeTemplate>) {
        function().forEach { template ->
            builder.add(template.codeBlock)
        }
    }

    fun collectStatements(function: () -> Collection<String>) {
        function().forEach { statement ->
            builder.addStatement(statement)
        }
    }

    fun code(function: () -> String) {
        builder.add(function())
    }

    companion object {
        fun code(function: () -> CodeBlock): CodeTemplate {
            return CodeTemplate(function())
        }
    }
}
