package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.generationdsl.dsl.KotlinTemplate
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate.Companion.parameterTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.STORE_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.STORE_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.TRANSACT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.UPDATE_DEPENDENCIES_PARAM_NAME
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asClassName
import getNullabilityExtension

internal fun createStoreFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate {
    return FunctionTemplate(
        name = STORE_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className
    ) {
        override()
        visibility { KotlinTemplate.Visibility.PROTECTED }
        parameterTemplate(name = UPDATE_DEPENDENCIES_PARAM_NAME, typeName = Boolean::class.asClassName())

        generateMethodBody {
            generateCodeTemplate { createStoreMethodBody(accessorClassInfo) }
        }
    }
}

private fun createStoreMethodBody(accessorClassInfo: AccessorClassInfo): CodeTemplate = CodeTemplate {
    val classSimpleName = accessorClassInfo.className.simpleName

    generateControlFlowCode("if (%L) ", UPDATE_DEPENDENCIES_PARAM_NAME) {
        generateCode("%L()", STORE_DEPENDENCIES_METHOD_SIMPLE_NAME)
    }

    generateNewLine()

    generateControlFlowCode(
        prefix = "return dbEntity.apply ",
        suffix = ".$EXPORT_PETAL_METHOD_SIMPLE_NAME()",
        endFlowString = "}"
    ) {
        collectCodeLines {
            accessorClassInfo.petalValueColumns.map { column ->
                val name = column.name
                "$name = this@${classSimpleName}.${name}"
            }
        }

        if (accessorClassInfo.petalValueColumns.size > 1) {
            generateNewLine()
        }

        collectCodeLines {
            accessorClassInfo.petalReferenceColumns.map { column ->
                val name = column.name
                val entityName = "${classSimpleName}.${name}" + column.getNullabilityExtension()
                "if·(${column.nestedPetalManagerName}.hasUpdated)·{·$name·=·this@${entityName}.dbEntity·}"
            }
        }
    }
}

internal fun createStoreDependenciesFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(name = STORE_DEPENDENCIES_METHOD_SIMPLE_NAME) {
        visibility { KotlinTemplate.Visibility.PRIVATE }

        generateMethodBody {
            collectCodeTemplates {
                accessorClassInfo.petalReferenceColumns
                    .map {
                        val name = it.name + it.getNullabilityExtension()
                        CodeTemplate("${name}.store(performInsideStandaloneTransaction = false)")
                    }
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

        generateMethodBody {
            generateControlFlowCode("return·apply·") {
                generateCode("·%M·{·statement()·}·", TRANSACTION_MEMBER_NAME)
            }
        }
    }
