package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorPropertyTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorPropertyTemplate.Companion.createConstructorPropertyTemplate
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class AccessorPropertiesBuilder(val accessorClassInfo: AccessorClassInfo) {

    val properties: Iterable<PropertySpec> by lazy {
        accessorClassInfo.petalColumns
            .filterIsInstance<PetalValueColumn>()
            .map { it.toPropertySpec() }
    }
}

internal fun TypeSpec.Builder.addAccessorProperties(accessorClassInfo: AccessorClassInfo) = apply {
    this.addProperties(AccessorPropertiesBuilder(accessorClassInfo).properties)
}

internal fun PetalValueColumn.toConstructorPropertyTemplate(): ConstructorPropertyTemplate {
    return createConstructorPropertyTemplate(
        name = name,
        typeName = tablePropertyClassName.copy(nullable = isNullable),
        isMutable = true
    ) {
        initializer { CodeTemplate(name) }
    }
}

private fun PetalValueColumn.toPropertySpec(): PropertySpec {
    val propertyTypeName = kotlinType.copy(nullable = isNullable)
    return PropertySpec.builder(name, propertyTypeName)
        .mutable()
        .initializer(name)
        .build()
}
