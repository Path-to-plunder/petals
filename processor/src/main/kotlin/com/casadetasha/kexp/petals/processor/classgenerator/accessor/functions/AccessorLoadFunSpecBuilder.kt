package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.AccessorCreateFunSpecBuilder.Companion.TRANSACTION_MEMBER_NAME
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import org.jetbrains.exposed.sql.SizedIterable

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorLoadFunSpecBuilder(accessorClassInfo: AccessorClassInfo) {

    private val loadFunSpec by lazy {
        FunSpec.builder(LOAD_METHOD_SIMPLE_NAME)
            .addParameter(ParameterSpec.builder("id", accessorClassInfo.idKotlinClassName).build())
            .returns(accessorClassInfo.className.copy(nullable = true))
            .addCode(loadMethodBody)
            .build()
    }

    private val loadMethodBody: CodeBlock by lazy {
        CodeBlock.builder()
            .beginControlFlow("return %M", TRANSACTION_MEMBER_NAME)
            .addStatement("%M.findById(id)", accessorClassInfo.entityMemberName)
            .unindent()
            .add("}?.export()")
            .build()
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
