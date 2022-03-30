package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetalColumn
import com.casadetasha.kexp.petals.processor.inputparser.PetalIdColumn
import com.casadetasha.kexp.petals.processor.inputparser.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.inputparser.PetalValueColumn
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorCreateFunSpecBuilder.Companion.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorStoreFunSpecBuilder(private val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

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

    private val nonIdColumns: Iterable<ParsedPetalColumn> by lazy {
        accessorClassInfo.petalColumns.filterNot { it is PetalIdColumn }
    }

    private val valueColumns: Iterable<PetalValueColumn> by lazy {
        nonIdColumns.filterIsInstance<PetalValueColumn>()
    }

    private val referenceColumns: Iterable<PetalReferenceColumn> by lazy {
        nonIdColumns.filterIsInstance<PetalReferenceColumn>()
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
                    val entityName = "${classSimpleName}.${name}" + if (column.isNullable) { "?" } else { "" }
                    addStatement("if (${column.nestedPetalManagerName}.hasUpdated) { $name = this@${entityName}.dbEntity }")
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
                referenceColumns
                    .forEach {
                        val name = it.name + if (it.isNullable) { "?" } else { "" }
                        addStatement("${name}.store(performInsideStandaloneTransaction = false)")
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

internal fun TypeSpec.Builder.addStoreMethod(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
    this.addFunctions(AccessorStoreFunSpecBuilder(accessorClassInfo).funSpecs)
}
