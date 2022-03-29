package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.addDefaultValueIfPresent
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

internal class AccessorClassInfoCreateFunParameterSpec(private val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

    val parameterSpecs: Iterable<ParameterSpec> by lazy {
        accessorClassInfo.columns
            .filterNot { it.isReferencedByColumn }
            .map { AccessorParameterSpecBuilder(it).parameterSpec }
    }

    private class AccessorParameterSpecBuilder(petalColumn: UnprocessedPetalColumn) {

        val parameterSpec: ParameterSpec by lazy {
            val propertyTypeName = when (petalColumn.isId) {
                true -> petalColumn.kotlinType.copy(nullable = true)
                false -> getNonIdTypeName(petalColumn)
            }
            val propertyBuilder = ParameterSpec.builder(petalColumn.name, propertyTypeName)

            when (petalColumn.isId) {
                true -> propertyBuilder.defaultValue("null")
                false -> propertyBuilder.addDefaultValueIfPresent(petalColumn.defaultValue)
            }
            propertyBuilder.addDefaultValueIfPresent(petalColumn.defaultValue)

            return@lazy propertyBuilder.build()
        }

        private fun getNonIdTypeName(column: UnprocessedPetalColumn): TypeName {
            return when (column.isReferenceColumn) {
                true -> column.referencingAccessorClassName!!.copy(nullable = column.isNullable)
                else -> column.kotlinType.copy(nullable = column.isNullable)
            }
        }
    }
}
