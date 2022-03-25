package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.AccessorCreateFunSpecBuilder.Companion.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.AccessorExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorStoreFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    val funSpecs: Iterable<FunSpec> by lazy {
        setOf(
            storeInsideOfTransactionFunSpec,
            storeDependenciesFunSpec,
            transactFunSpec
        )
    }

    private val storeInsideOfTransactionFunSpec: FunSpec by lazy {
        FunSpec.builder(STORE_METHOD_SIMPLE_NAME)
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
            .addCode(storeMethodBody)
            .build()
    }

    private val nonIdColumns: Iterable<UnprocessedPetalColumn> by lazy {
        accessorClassInfo.columns.filterNot { it.isId }
    }

    private val valueColumns: Iterable<UnprocessedPetalColumn> by lazy {
        nonIdColumns.filterNot { it.isReferenceColumn }
            .filterNot { it.isReferencedByColumn }
    }

    private val referenceColumns: Iterable<UnprocessedPetalColumn> by lazy {
        nonIdColumns.filter { it.isReferenceColumn }
    }

    private val storeMethodBody: CodeBlock by lazy {
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
                    addStatement("if (${column.nestedPetalManagerName}.hasUpdated) { $name = this@${classSimpleName}.${name}.dbEntity }")
                }
            }
            .unindent()
            .add("}.$EXPORT_METHOD_SIMPLE_NAME()")
            .build()
    }

    private val storeDependenciesFunSpec: FunSpec by lazy {
        FunSpec.builder(STORE_DEPENDENCIES_METHOD_SIMPLE_NAME)
            .addModifiers(KModifier.PRIVATE)
            .apply {
                accessorClassInfo.columns
                    .filter { it.isReferenceColumn }
                    .forEach {
                        addStatement("${it.name}.store(performInsideStandaloneTransaction = false)")
                    }
            }
            .build()
    }

    private val transactFunSpec: FunSpec by lazy {
        FunSpec.builder(TRANSACT_METHOD_SIMPLE_NAME)
            .addModifiers(
                KModifier.OVERRIDE
            )
            .returns(accessorClassInfo.className)
            .addParameter(
                ParameterSpec.builder(
                    name = "statement",
                    type = LambdaTypeName.get(
                        receiver = accessorClassInfo.className,
                        returnType = Unit::class.asClassName()
                    )
                ).build()
            )
            .addStatement("return·apply·{·%M·{·statement()·}·}", TRANSACTION_MEMBER_NAME)
            .build()
    }

    companion object {
        const val STORE_METHOD_SIMPLE_NAME = "storeInsideOfTransaction"
        const val STORE_DEPENDENCIES_METHOD_SIMPLE_NAME = "storeDependencies"
        const val TRANSACT_METHOD_SIMPLE_NAME = "applyInsideTransaction"

        const val UPDATE_DEPENDENCIES_PARAM_NAME = "updateNestedDependencies"
    }
}

internal fun TypeSpec.Builder.addStoreMethod(accessorClassInfo: AccessorClassInfo) = apply {
    this.addFunctions(AccessorStoreFunSpecBuilder(accessorClassInfo).funSpecs)
}
