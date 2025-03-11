package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.generationdsl.dsl.KotlinModifiers
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.STORE_ACCESSOR_PARAM_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.STORE_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.STORE_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.TRANSACT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.StoreMethodNames.UPDATE_DEPENDENCIES_PARAM_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.toMemberName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.asClassName
import getNullabilityExtension

internal fun createStoreFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate {
    return FunctionTemplate(
        name = STORE_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className
    ) {
        override()
        visibility { KotlinModifiers.Visibility.PROTECTED }
        collectParameterTemplates {
            listOf(
                ParameterTemplate(name = STORE_ACCESSOR_PARAM_NAME, typeName = accessorClassInfo.className),
                ParameterTemplate(name = UPDATE_DEPENDENCIES_PARAM_NAME, typeName = Boolean::class.asClassName()),
            )
        }

        generateMethodBody {
            generateCodeTemplate { createStoreMethodBody(accessorClassInfo) }
        }
    }
}

private fun createStoreMethodBody(accessorClassInfo: AccessorClassInfo): CodeTemplate = CodeTemplate {
    generateControlFlowCode("if (%L) ", UPDATE_DEPENDENCIES_PARAM_NAME) {
        generateCode("$STORE_ACCESSOR_PARAM_NAME.%L()", STORE_DEPENDENCIES_METHOD_SIMPLE_NAME)
    }

    generateNewLine()
    generateNewLine()

    generateControlFlowCode(
        prefix = "return ${STORE_ACCESSOR_PARAM_NAME}.dbEntity.apply ",
        suffix = ".$EXPORT_PETAL_METHOD_SIMPLE_NAME()",
        endFlowString = "}"
    ) {
        if (accessorClassInfo.hasTimestamps) {
            generateCode("val timestamp = clock.instant().toEpochMilli()")
            generateNewLine()
        }

        collectCodeLines {
            setOf(
                accessorClassInfo.petalValueColumns.map { column ->
                    val name = column.name
                    "$name = ${STORE_ACCESSOR_PARAM_NAME}.${name}"
                },
                accessorClassInfo.timestampColumns.map { column ->
                    "${column.name} = timestamp"
                }
            ).flatten()
        }

        if (accessorClassInfo.petalValueColumns.size > 1) {
            generateNewLine()
        }

        collectCodeLines {
            accessorClassInfo.petalReferenceColumns.map { column ->
                val name = column.name
                val entityName = "${STORE_ACCESSOR_PARAM_NAME}.${name}" + column.getNullabilityExtension()
                "if·(${STORE_ACCESSOR_PARAM_NAME}.${column.nestedPetalManagerName}.hasUpdated)·{·$name·=·${entityName}.dbEntity·}"
            }
        }
    }
}

internal fun createStoreDependenciesFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(name = STORE_DEPENDENCIES_METHOD_SIMPLE_NAME) {
        visibility { KotlinModifiers.Visibility.PRIVATE }

        generateMethodBody {
            collectCodeTemplates {
                accessorClassInfo.petalReferenceColumns
                    .map {
                        val name = it.name + it.getNullabilityExtension()
                        CodeTemplate("${name}.let { %M.store(it, performInsideStandaloneTransaction = false) }", it.referencingAccessorClassName.toMemberName())
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

        generateParameter(
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
