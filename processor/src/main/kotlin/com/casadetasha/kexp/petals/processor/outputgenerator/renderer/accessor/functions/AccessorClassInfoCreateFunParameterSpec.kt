package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.inputparser.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.inputparser.PetalIdColumn
import com.casadetasha.kexp.petals.processor.inputparser.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.inputparser.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.addDefaultValueIfPresent
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName

internal class AccessorClassInfoCreateFunParameterSpec(private val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

    val parameterSpecs: Iterable<ParameterSpec> by lazy {
        accessorClassInfo.petalColumns
            .filterIsInstance<LocalPetalColumn>()
            .map { AccessorParameterSpecBuilder(it).parameterSpec }
    }

    private class AccessorParameterSpecBuilder(petalColumn: LocalPetalColumn) {

        val parameterSpec: ParameterSpec by lazy {
            val propertyTypeName = when (petalColumn is PetalIdColumn) {
                true -> petalColumn.kotlinType.copy(nullable = true)
                false -> getNonIdTypeName(petalColumn)
            }
            val propertyBuilder = ParameterSpec.builder(petalColumn.name, propertyTypeName)

            when (petalColumn) {
                is PetalIdColumn -> propertyBuilder.defaultValue("null")
                is PetalValueColumn -> propertyBuilder.addDefaultValueIfPresent(petalColumn.defaultValue)
                else -> { /* do nothing */ }
            }

            return@lazy propertyBuilder.build()
        }

        private fun getNonIdTypeName(column: LocalPetalColumn): TypeName {
            return when (column) {
                is PetalReferenceColumn -> column.referencingAccessorClassName.copy(nullable = column.isNullable)
                else -> column.kotlinType.copy(nullable = column.isNullable)
            }
        }
    }
}
