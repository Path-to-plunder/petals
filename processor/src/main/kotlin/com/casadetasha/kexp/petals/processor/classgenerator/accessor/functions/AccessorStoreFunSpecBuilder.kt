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
                    .filter { it.columnReference != null }
                    .forEach {
                        addStatement("${it.name}.store()")
                    }
            }
            .build()
    }

    fun getFunSpecs(): Iterable<FunSpec> {
        return setOf(
            createStoreInsideOfTransactionFunSpec(),
            storeDependenciesFunSpec
        )
    }

    private fun createStoreInsideOfTransactionFunSpec(): FunSpec {
        return FunSpec.builder(STORE_METHOD_SIMPLE_NAME)
            .addModifiers(
                KModifier.OVERRIDE,
                KModifier.PROTECTED
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

        val valueColumns: Iterable<UnprocessedPetalColumn> by lazy {
            nonIdColumns.filter { it.columnReference == null }
        }

        val referenceColumns: Iterable<UnprocessedPetalColumn> by lazy {
            nonIdColumns.filterNot { it.columnReference == null }
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
                    valueColumns.forEach { column ->
                        val name = column.name
                        addStatement("$name = this@${classSimpleName}.${name}")
                    }
                }
                .apply {
                    referenceColumns.forEach { column ->
                        val name = column.name
                        addStatement("if (${column.nestedPetalManagerName}.hasUpdated()) { $name = this@${classSimpleName}.${name}.dbEntity }")
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
