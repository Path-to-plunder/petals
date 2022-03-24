package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.AccessorCreateFunSpecBuilder.Companion.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.kotlinpoet.createParameter
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.jetbrains.exposed.sql.SizedIterable

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorLoadFunSpecBuilder(val accessorClassInfo: AccessorClassInfo) {

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
                className = Boolean::class.asClassName(),
                defaultValue = CodeBlock.of("false"))
        )
    }

    private val loadMethodBody: CodeBlock by lazy {
        when (accessorClassInfo.columns.any { it.isReferenceColumn }) {
            true -> CodeBlock.builder()
                    .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                    .beginControlFlow("when (eagerLoad)")
                    .addStatement("true -> %M.findById(id)?.exportWithEagerLoadedDependencies()", accessorClassInfo.entityMemberName)
                    .addStatement("false -> %M.findById(id)?.export()", accessorClassInfo.entityMemberName)
                    .endControlFlow()
                    .endControlFlow()
                    .build()
            false -> CodeBlock.builder()
                .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
                .addStatement("%M.findById(id)", accessorClassInfo.entityMemberName)
                .unindent()
                .add("}?.export()")
                .build()
        }
    }


    private val loadAllFunSpec by lazy {
        FunSpec.builder(LOAD_ALL_METHOD_SIMPLE_NAME)
            .returns(
                List::class.asClassName()
                    .parameterizedBy(accessorClassInfo.className)
            )
            .addCode(loadAllMethodBody)
            .build()
    }

    private val loadAllMethodBody: CodeBlock by lazy {
        CodeBlock.builder()
            .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
            .addStatement(
                "%M.all().map { it.export() }",
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
            .addCode(lazyLoadAllMethodBody)
            .build()
    }

    private val lazyLoadAllMethodBody: CodeBlock by lazy {
        CodeBlock.builder()
            .addStatement(
                "return %M.all().%M { it.export() }",
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
