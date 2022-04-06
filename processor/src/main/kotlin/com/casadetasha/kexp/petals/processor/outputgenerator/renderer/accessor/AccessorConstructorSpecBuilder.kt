package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec

internal class AccessorConstructorSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    val constructorSpec: FunSpec by lazy {
        val constructorBuilder = FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameter(getDbEntityParamSpec())

        accessorClassInfo.petalColumns
            .filterIsInstance<LocalPetalColumn>()
            .forEach { constructorBuilder.addParameter(getParameterSpec(it)) }
        return@lazy constructorBuilder.build()
    }

    private fun getDbEntityParamSpec(): ParameterSpec = ParameterSpec
        .builder("dbEntity", accessorClassInfo.entityClassName)
        .build()

    private fun getParameterSpec(column: LocalPetalColumn): ParameterSpec {
        val propertyTypeName = when (column) {
            is PetalIdColumn -> column.kotlinType
            else -> column.kotlinType.copy(nullable = column.isNullable)
        }

        val name = when (column) {
            is PetalReferenceColumn -> "${column.name}Id"
            else -> column.name
        }

        val builder = ParameterSpec.builder(name, propertyTypeName)
        if (column is PetalValueColumn) {
            builder.addDefaultValue(column.defaultValue)
        }

        if (column is PetalReferenceColumn && column.isNullable) {
            builder.defaultValue("null")
        }

        return builder.build()
    }
}
