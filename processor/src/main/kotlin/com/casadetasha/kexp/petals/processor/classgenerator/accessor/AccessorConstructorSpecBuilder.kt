package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec

internal class AccessorConstructorSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    val constructorSpec: FunSpec by lazy {
        val constructorBuilder = FunSpec.constructorBuilder()
            .addModifiers(KModifier.PRIVATE)
            .addParameter(getDbEntityParamSpec())

        accessorClassInfo.sortedColumns
            .filterNot { it.isReferencedByColumn }
            .forEach {
            constructorBuilder.addParameter(getParameterSpec(it))
        }
        return@lazy constructorBuilder.build()
    }

    private fun getDbEntityParamSpec(): ParameterSpec = ParameterSpec
        .builder("dbEntity", accessorClassInfo.entityClassName)
        .build()

    private fun getParameterSpec(column: UnprocessedPetalColumn): ParameterSpec {
        val propertyTypeName = when (column.isId) {
            true -> column.kotlinType
            false -> column.kotlinType.copy(nullable = column.isNullable)
        }

        val name = when (column.isReferenceColumn) {
            true -> "${column.name}Id"
            false -> column.name
        }

        val builder = ParameterSpec.builder(name, propertyTypeName)
        return builder.addDefaultValueIfPresent(column.defaultValue)
            .build()
    }
}
