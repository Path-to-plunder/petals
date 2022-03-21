package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.NestedPetalManager
import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class NestedPetalPropertySpecListBuilder(
    private val accessorClassInfo: AccessorClassInfo,
    private val column: UnprocessedPetalColumn
) {

    private val nestedPetalManagerPropertySpec: PropertySpec by lazy {
        val entityManagerClassName = NestedPetalManager::class.asClassName()
            .parameterizedBy(
                column.referencingAccessorClassName!!,
                column.referencingEntityClassName!!,
                column.kotlinType
            )

        PropertySpec.builder(column.nestedPetalManagerName!!, entityManagerClassName)
            .addModifiers(KModifier.PRIVATE)
            .delegate(petalManagerMethodBody)
            .build()
    }

    private val petalManagerMethodBody: CodeBlock by lazy {
        CodeBlock.builder()
            .beginControlFlow("lazy")
            .addStatement(
                "%M(%L) { dbEntity.%L.%M() }",
                NestedPetalManager::class.asClassName().toMemberName(),
                column.referencingIdName,
                column.name,
                ClassName( "${column.referencingAccessorClassName!!.packageName}.${column.referencingAccessorClassName.simpleName}.Companion",
                    "export"
                ).toMemberName()
            )
            .endControlFlow()
            .build()
    }

    private val nestedPetalIdPropertySpec: PropertySpec by lazy {
        PropertySpec.builder(column.referencingIdName!!, column.kotlinType)
            .delegate("${column.nestedPetalManagerName}::nestedPetalId")
            .build()
    }

    private val nestedPetalAccessorPropertySpec: PropertySpec by lazy {
        PropertySpec.builder(column.name, column.referencingAccessorClassName!!)
            .delegate("${column.nestedPetalManagerName}::nestedPetal")
            .build()
    }


    fun getPropertySpecs(): List<PropertySpec> {
        checkReferenceNotNull()

        return listOf(
            nestedPetalManagerPropertySpec,
            nestedPetalIdPropertySpec,
            nestedPetalAccessorPropertySpec
        )

    }

    private fun checkReferenceNotNull() {
        if (column.columnReference == null) {
            AnnotationParser.printThenThrowError(
                "INTERNAL LIBRARY ERROR: Attempting to add delegate properties to column without a" +
                        " reference. Petal name ${accessorClassInfo.className}, column name: ${column.name}.")
        }
    }
}

internal fun ClassName.toMemberName(): MemberName = MemberName(packageName, simpleName)

internal fun TypeSpec.Builder.addNestedPetalPropertySpec(accessorClassInfo: AccessorClassInfo) = apply {
    accessorClassInfo.columns
        .filter { it.columnReference != null }
        .forEach {
            addProperties(NestedPetalPropertySpecListBuilder(accessorClassInfo, it).getPropertySpecs())
        }
}
