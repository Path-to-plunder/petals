package com.casadetasha.kexp.petals.processor.kotlinpoet

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asClassName

internal fun createParameter(name: String, className: ClassName) = ParameterSpec.builder(name, className).build()

internal fun createParameter(name: String, className: ClassName, defaultValue: CodeBlock): ParameterSpec {
    return ParameterSpec.builder(name, className)
        .defaultValue(defaultValue)
        .build()
}
