package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.inputparser.PetalValueColumn
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class AccessorPropertiesBuilder(val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

    val properties: Iterable<PropertySpec> by lazy {
        accessorClassInfo.petalColumns
            .filterIsInstance<PetalValueColumn>()
            .map { it.toPropertySpec() }
    }
}

internal fun TypeSpec.Builder.addAccessorProperties(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
    this.addProperties(AccessorPropertiesBuilder(accessorClassInfo).properties)
}

private fun PetalValueColumn.toPropertySpec(): PropertySpec {
    val propertyTypeName = kotlinType.copy(nullable = isNullable)
    return PropertySpec.builder(name, propertyTypeName)
        .mutable()
        .initializer(name)
        .build()
}
