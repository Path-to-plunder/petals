package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data

import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.outputgenerator.kotlinpoet.createParameter
import com.casadetasha.kexp.petals.processor.outputgenerator.kotlinpoet.createConstructorProperty
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import java.util.*
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

@OptIn(KotlinPoetMetadataPreview::class)
internal class DataClassSpecBuilder(val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

    internal fun getClassSpec(): TypeSpec {
        return TypeSpec.classBuilder(accessorClassInfo.dataClassName)
            .addAnnotation(serializableAnnotation)
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

    private val parameterSpecs: Iterable<ParameterSpec> = accessorClassInfo.sortedColumns
        .filterNot { it.isReferencedByColumn }
        .map { column ->
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

    private val propertySpecs: Iterable<PropertySpec> = accessorClassInfo.sortedColumns
        .filterNot { it.isReferencedByColumn }
        .map { column ->
        val typeName = when (column.isId) {
            true -> column.kotlinType
            false -> column.kotlinType.copy(nullable = column.isNullable)
        }

        val name = when (column.isReferenceColumn) {
            false -> column.name
            true -> "${column.name}Id"
        }

        val annotations = when (column.kotlinType) {
            UUID::class.asClassName() -> listOf(uuidSerializableAnnotation)
            else -> emptyList()
        }

        return@map createConstructorProperty(
            name = name,
            typeName = typeName,
            isMutable = column.isMutable,
            annotations = annotations
        )
    }

    private val uuidSerializableAnnotation: AnnotationSpec get() {
        return AnnotationSpec.builder(Serializable::class)
            .addMember("with = %M::class", UUIDSerializer::class.asMemberName())
            .build()
    }

    private val serializableAnnotation: AnnotationSpec get() {
        return AnnotationSpec.builder(Serializable::class).build()
    }
}

private fun KClass<*>.asMemberName(): MemberName =
    asClassName().run { MemberName(packageName, simpleName) }
