package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.kotlinpoet.createParameter
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorCreateFunSpecBuilder.Companion.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorEagerLoadDependenciesFunSpecBuilder.Companion.COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.jetbrains.exposed.sql.SizedIterable

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorLoadFunSpecBuilder(val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

    private val loadFunSpec by lazy {
        FunSpec.builder(LOAD_METHOD_SIMPLE_NAME)
            .addParameters(loadMethodParameters)
            .returns(accessorClassInfo.className.copy(nullable = true))
            .addCode(loadMethodBody)
            .build()
    }

    private val loadMethodParameters by lazy {
        listOf(
            createParameter("id", accessorClassInfo.idKotlinClassName),
            createParameter(
                name = "eagerLoad",
                typeName = Boolean::class.asClassName(),
                defaultValue = CodeBlock.of("false"))
        )
    }

    private val loadMethodBody: CodeBlock by lazy {
        when (accessorClassInfo.petalColumns.any { it is PetalReferenceColumn }) {
            true -> CodeBlock.builder()
                    .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                    .beginControlFlow("when (eagerLoad)")
                    .addStatement(
                        "true -> %M.findById(id)?.$COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME()",
                        accessorClassInfo.entityMemberName)
                    .addStatement("false -> %M.findById(id)?.$EXPORT_METHOD_SIMPLE_NAME()", accessorClassInfo.entityMemberName)
                    .endControlFlow()
                    .endControlFlow()
                    .build()
            false -> CodeBlock.builder()
                .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                .addStatement("%M.findById(id)", accessorClassInfo.entityMemberName)
                .unindent()
                .add("}?.$EXPORT_METHOD_SIMPLE_NAME()")
                .build()
        }
    }


    private val loadAllFunSpec by lazy {
        FunSpec.builder(LOAD_ALL_METHOD_SIMPLE_NAME)
            .returns(
                List::class.asClassName()
                    .parameterizedBy(accessorClassInfo.className)
            )
            .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
            .addStatement(
                "%M.all().map { it.$EXPORT_METHOD_SIMPLE_NAME() }",
                accessorClassInfo.entityMemberName,
            )
            .endControlFlow()
            .build()
    }

    private val lazyLoadAllFunSpec by lazy {
        FunSpec.builder(LAZY_LOAD_ALL_METHOD_SIMPLE_NAME)
            .returns(
                SizedIterable::class.asClassName()
                    .parameterizedBy(accessorClassInfo.className)
            )
            .addStatement(
                "return %M.all().%M { it.$EXPORT_METHOD_SIMPLE_NAME() }",
                accessorClassInfo.entityMemberName,
                MAP_LAZY_MEMBER_NAME
            )
            .build()
    }

    val loadFunSpecs = listOf(
        loadFunSpec,
        loadAllFunSpec,
        lazyLoadAllFunSpec
    )

    companion object {
        const val LOAD_METHOD_SIMPLE_NAME = "load"
        const val LOAD_ALL_METHOD_SIMPLE_NAME = "loadAll"
        const val LAZY_LOAD_ALL_METHOD_SIMPLE_NAME = "lazyLoadAll"
        val MAP_LAZY_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql", "mapLazy")
    }
}
