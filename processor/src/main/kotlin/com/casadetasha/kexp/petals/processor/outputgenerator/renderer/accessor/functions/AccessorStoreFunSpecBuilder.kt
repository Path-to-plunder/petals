package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorCreateFunSpecBuilder.Companion.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.StoreMethodNames.STORE_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.StoreMethodNames.STORE_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.StoreMethodNames.TRANSACT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.StoreMethodNames.UPDATE_DEPENDENCIES_PARAM_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate.Companion.codeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.KotlinTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate.Companion.parameterTemplate
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asClassName

object StoreMethodNames {
    const val STORE_METHOD_SIMPLE_NAME = "storeInsideOfTransaction"
    const val STORE_DEPENDENCIES_METHOD_SIMPLE_NAME = "storeDependencies"
    const val TRANSACT_METHOD_SIMPLE_NAME = "applyInsideTransaction"

    const val UPDATE_DEPENDENCIES_PARAM_NAME = "updateNestedDependencies"
}

internal fun createStoreFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate {
    return FunctionTemplate(
        name = STORE_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className
    ) {
        override()

        parameterTemplate( name = UPDATE_DEPENDENCIES_PARAM_NAME, typeName = Boolean::class.asClassName() )

        visibility { KotlinTemplate.Visibility.PROTECTED }

        collectCode {
            listOf(
                createStoreMethodBody(accessorClassInfo)
            )
        }
    }
}


private fun createStoreMethodBody(accessorClassInfo: AccessorClassInfo): CodeTemplate = codeTemplate {
    val codeBlockBuilder = CodeBlock.builder()
    val classSimpleName = accessorClassInfo.className.simpleName

    codeBlockBuilder.addStatement(
        "if (%L) { %L() }\n",
        UPDATE_DEPENDENCIES_PARAM_NAME,
        STORE_DEPENDENCIES_METHOD_SIMPLE_NAME
    )
        .beginControlFlow("return dbEntity.apply ")
        .apply {
            accessorClassInfo.petalValueColumns.forEach { column ->
                val name = column.name
                addStatement("$name = this@${classSimpleName}.${name}")
            }
        }
        .apply {
            accessorClassInfo.petalReferenceColumns.forEach { column ->
                val name = column.name
                val entityName = "${classSimpleName}.${name}" + if (column.isNullable) {
                    "?"
                } else {
                    ""
                }
                addStatement("if (${column.nestedPetalManagerName}.hasUpdated) { $name = this@${entityName}.dbEntity }")
            }
        }
        .unindent()
        .add("}.$EXPORT_METHOD_SIMPLE_NAME()")
        .build()
}


internal fun createStoreDependenciesFunSpec(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(name = STORE_DEPENDENCIES_METHOD_SIMPLE_NAME) {
        visibility { KotlinTemplate.Visibility.PRIVATE }

        collectCode {
            accessorClassInfo.petalReferenceColumns
                .map {
                    val name = it.name + if (it.isNullable) {
                        "?"
                    } else {
                        ""
                    }
                    CodeTemplate("${name}.store(performInsideStandaloneTransaction = false)")
                }
        }
    }

internal fun createTransactFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(
        name = TRANSACT_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className
    ) {
        override()

        parameterTemplate(
            name = "statement",
            typeName = LambdaTypeName.get(
                receiver = accessorClassInfo.className,
                returnType = Unit::class.asClassName()
            )
        )

        writeCode("return·apply·{·%M·{·statement()·}·}", TRANSACTION_MEMBER_NAME)
    }

