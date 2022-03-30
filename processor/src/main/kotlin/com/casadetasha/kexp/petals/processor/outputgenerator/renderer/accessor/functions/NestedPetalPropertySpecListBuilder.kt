package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.NestedPetalManager
import com.casadetasha.kexp.petals.annotations.OptionalNestedPetalManager
import com.casadetasha.kexp.petals.processor.inputparser.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class NestedPetalPropertySpecListBuilder(
    private val column: PetalReferenceColumn
) {

    private val nestedPetalManagerClassName by lazy {
        when (column.isNullable) {
            false -> NestedPetalManager::class.asClassName()
            true -> OptionalNestedPetalManager::class.asClassName()
        }
    }

    private val nestedPetalManagerPropertySpec: PropertySpec by lazy {
        val entityManagerClassName = nestedPetalManagerClassName
            .parameterizedBy(
                column.referencingAccessorClassName,
                column.referencingEntityClassName,
                column.kotlinType
            )

        PropertySpec.builder(column.nestedPetalManagerName, entityManagerClassName)
            .addModifiers(KModifier.PRIVATE)
            .delegate(petalManagerMethodBody)
            .build()
    }

    private val petalManagerMethodBody: CodeBlock by lazy {
        CodeBlock.builder()
            .beginControlFlow("lazy")
            .addStatement(
                "%M(%L) { dbEntity.%L?.%M() }",
                nestedPetalManagerClassName.toMemberName(),
                column.referencingIdName,
                column.name,
                ClassName( "${column.referencingAccessorClassName.packageName}.${column.referencingAccessorClassName.simpleName}.Companion",
                    EXPORT_METHOD_SIMPLE_NAME
                ).toMemberName()
            )
            .endControlFlow()
            .build()
    }

    private val nestedPetalIdPropertySpec: PropertySpec by lazy {
        PropertySpec.builder(column.referencingIdName, column.kotlinType.copy(nullable = column.isNullable))
            .delegate("${column.nestedPetalManagerName}::nestedPetalId")
            .build()
    }

    private val nestedPetalAccessorPropertySpec: PropertySpec by lazy {
        PropertySpec.builder(column.name, column.referencingAccessorClassName.copy(nullable = column.isNullable))
            .delegate("${column.nestedPetalManagerName}::nestedPetal")
            .mutable()
            .build()
    }


    fun getPropertySpecs(): List<PropertySpec> {
        return listOf(
            nestedPetalManagerPropertySpec,
            nestedPetalIdPropertySpec,
            nestedPetalAccessorPropertySpec
        )

    }
}

internal fun ClassName.toMemberName(): MemberName = MemberName(packageName, simpleName)

internal fun TypeSpec.Builder.addNestedPetalPropertySpec(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
    accessorClassInfo.petalColumns
        .filterIsInstance<PetalReferenceColumn>()
        .forEach {
            addProperties(NestedPetalPropertySpecListBuilder(it).getPropertySpecs())
        }
}
