package com.casadetasha.kexp.petals.processor.classgenerator.data

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.kotlinpoet.createParameter
import com.casadetasha.kexp.petals.processor.kotlinpoet.createConstructorProperty
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class DataClassSpecBuilder(val accessorClassInfo: AccessorClassInfo) {

    internal fun getClassSpec(): TypeSpec {
        return TypeSpec.classBuilder(accessorClassInfo.className)
            .addModifiers(KModifier.DATA)
            .primaryConstructor(constructorSpec)
            .addProperties(propertySpecs)
            .build()
    }

    private val constructorSpec: FunSpec by lazy {
        val constructorBuilder = FunSpec.constructorBuilder()

            constructorBuilder.addParameters(parameterSpecs)
        return@lazy constructorBuilder.build()
    }

    private val parameterSpecs: Iterable<ParameterSpec> = accessorClassInfo.sortedColumns.map { column ->
        val typeName = when (column.isId) {
            true -> column.kotlinType
            false -> column.kotlinType.copy(nullable = column.isNullable)
        }

        val name = when (column.columnReferenceInfo) {
            null -> column.name
            else -> "${column.name}Id"
        }

        return@map createParameter(name, typeName)
    }

    private val propertySpecs: Iterable<PropertySpec> = accessorClassInfo.sortedColumns.map { column ->
        val typeName = when (column.isId) {
            true -> column.kotlinType
            false -> column.kotlinType.copy(nullable = column.isNullable)
        }

        val name = when (column.columnReferenceInfo) {
            null -> column.name
            else -> "${column.name}Id"
        }

        return@map createConstructorProperty(name, typeName, isMutable = column.isMutable)
    }
}


