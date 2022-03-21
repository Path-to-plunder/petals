package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class AccessorPropertiesBuilder(val accessorClassInfo: AccessorClassInfo) {

    val properties: Iterable<PropertySpec> by lazy {
        accessorClassInfo.sortedColumns
            .filterNot { it.isId }
            .map { it.toPropertySpec() }
    }
}

internal fun TypeSpec.Builder.addAccessorProperties(accessorClassInfo: AccessorClassInfo) = apply {
    this.addProperties(AccessorPropertiesBuilder(accessorClassInfo).properties)
}

private fun UnprocessedPetalColumn.toPropertySpec(): PropertySpec {
    val propertyTypeName = kotlinType.copy(nullable = isNullable)
    return PropertySpec.builder(name, propertyTypeName)
        .mutable()
        .initializer(name)
        .build()
}
