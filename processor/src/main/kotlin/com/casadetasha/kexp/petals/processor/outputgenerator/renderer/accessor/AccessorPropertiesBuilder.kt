package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalColumn
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

internal class AccessorPropertiesBuilder(val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

    val properties: Iterable<PropertySpec> by lazy {
        accessorClassInfo.sortedColumns
            .filterNot { it.isId }
            .filterNot { it.isReferenceColumn }
            .filterNot { it.isReferencedByColumn }
            .map { it.toPropertySpec() }
    }
}

internal fun TypeSpec.Builder.addAccessorProperties(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
    this.addProperties(AccessorPropertiesBuilder(accessorClassInfo).properties)
}

private fun UnprocessedPetalColumn.toPropertySpec(): PropertySpec {
    val propertyTypeName = kotlinType.copy(nullable = isNullable)
    return PropertySpec.builder(name, propertyTypeName)
        .mutable()
        .initializer(name)
        .build()
}
