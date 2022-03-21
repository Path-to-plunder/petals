package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorStoreFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    companion object {
        const val STORE_METHOD_SIMPLE_NAME = "storeInsideOfTransaction"
        const val STORE_DEPENDENCIES_METHOD_SIMPLE_NAME = "storeDependencies"

        const val UPDATE_DEPENDENCIES_PARAM_NAME = "updateNestedDependencies"
    }

    private val storeDependenciesFunSpec: FunSpec by lazy {
        FunSpec.builder(STORE_DEPENDENCIES_METHOD_SIMPLE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .apply {
                accessorClassInfo.columns
                    .filter { it.referencing != null }
                    .forEach {
                        addStatement("${it.name}.store()")
                    }
            }
            .build()
    }

    fun getFunSpecs(): Iterable<FunSpec> {
        return setOf(
            createSetValuesFunction(),
            storeDependenciesFunSpec
        )
    }

    private fun createSetValuesFunction(): FunSpec {
        return FunSpec.builder(STORE_METHOD_SIMPLE_NAME)
            .addModifiers(
                KModifier.OVERRIDE
            )
            .addParameter(
                ParameterSpec.builder(
                    name = UPDATE_DEPENDENCIES_PARAM_NAME,
                    type = Boolean::class.asClassName()
                ).build())
            .returns(accessorClassInfo.className)
            .addCode(SetValuesFunctionParser(accessorClassInfo).methodBody)
            .build()
    }

    private class SetValuesFunctionParser(accessorClassInfo: AccessorClassInfo) {

        val nonIdColumns: Iterable<UnprocessedPetalColumn> by lazy {
            accessorClassInfo.columns.filterNot { it.isId }
        }

        val methodBody: CodeBlock by lazy {
            val codeBlockBuilder = CodeBlock.builder()
            val classSimpleName = accessorClassInfo.className.simpleName

            codeBlockBuilder.addStatement(
                "if (%L) { %L() }\n",
                UPDATE_DEPENDENCIES_PARAM_NAME,
                STORE_DEPENDENCIES_METHOD_SIMPLE_NAME
            )
                .beginControlFlow("return dbEntity.apply ")
                .apply {
                    nonIdColumns.forEach { column ->
                        val name = column.name
                        addStatement("$name = this@${classSimpleName}.$name")
                    }
                }
                .unindent()
                .add("}.export()")
                .build()
        }
    }
}

internal fun TypeSpec.Builder.addStoreMethod(accessorClassInfo: AccessorClassInfo) = apply {
    this.addFunctions(AccessorStoreFunSpecBuilder(accessorClassInfo).getFunSpecs())
}
