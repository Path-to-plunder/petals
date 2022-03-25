package com.casadetasha.kexp.petals.processor.kotlinpoet

import com.squareup.kotlinpoet.*

internal fun createParameter(name: String, typeName: TypeName) =
    ParameterSpec.builder(name, typeName)
        .build()

internal fun createParameter(name: String, typeName: TypeName, defaultValue: CodeBlock): ParameterSpec =
    ParameterSpec.builder(name, typeName)
        .defaultValue(defaultValue)
        .build()

internal fun createConstructorProperty(name: String, typeName: TypeName, isMutable: Boolean = false) =
    PropertySpec.builder(name, typeName)
        .mutable(mutable = isMutable)
        .initializer(name)
        .build()
