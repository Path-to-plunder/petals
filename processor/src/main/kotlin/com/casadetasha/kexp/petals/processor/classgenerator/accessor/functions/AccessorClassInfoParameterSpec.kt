package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.addDefaultValueIfPresent
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

internal class AccessorClassInfoParameterSpec(private val accessorClassInfo: AccessorClassInfo) {

    val parameterSpecs: Iterable<ParameterSpec> by lazy {
        accessorClassInfo.columns
            .map { AccessorParameterSpecBuilder(it).parameterSpec }
    }

    private class AccessorParameterSpecBuilder(petalColumn: UnprocessedPetalColumn) {

        val parameterSpec: ParameterSpec by lazy {
            val propertyTypeName = when (petalColumn.isId) {
                true -> petalColumn.kotlinType.copy(nullable = true)
                false -> getNonIdTypeName(petalColumn)
            }
            val propertyBuilder = ParameterSpec.builder(petalColumn.name, propertyTypeName)

            propertyBuilder.addDefaultValueIfPresent(petalColumn.defaultValue)

            return@lazy propertyBuilder.build()
        }

        private fun getNonIdTypeName(column: UnprocessedPetalColumn): TypeName {
            return when (column.columnReference) {
                null -> column.kotlinType.copy(nullable = column.isNullable)
                else -> column.referencingAccessorClassName!!.copy(nullable = column.isNullable)
            }
        }
    }
}
