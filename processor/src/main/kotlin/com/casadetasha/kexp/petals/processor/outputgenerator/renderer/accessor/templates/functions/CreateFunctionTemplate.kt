package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.CREATE_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.asCreateFunctionParameterTemplate
import getNullabilityExtension

internal fun createCreateFunctionTemplate(accessorClassInfo: AccessorClassInfo) =
    FunctionTemplate(
        name = CREATE_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className
    ) {
        collectParameterTemplates {
            accessorClassInfo.localColumns.map { it.asCreateFunctionParameterTemplate() }
        }

        generateMethodBody(createCreateFunctionMethodBodyTemplate(accessorClassInfo))
    }

private fun createCreateFunctionMethodBodyTemplate(accessorClassInfo: AccessorClassInfo): CodeTemplate {
    val entitySimpleName = accessorClassInfo.entityClassName.simpleName
    return CodeTemplate {
        generateControlFlowCode(
            prefix = "return %M", TRANSACTION_MEMBER_NAME,
            suffix = ".$EXPORT_PETAL_METHOD_SIMPLE_NAME()",
            endFlowString = "}"
        ) {
            generateControlFlowCode("val storeValues: %L.() -> Unit = ", entitySimpleName,
                endFlowString = "}"
            ) {
                generateCodeTemplate { createAssignAccessorValuesCodeBlock(accessorClassInfo) }
            }

            generateNewLine()
            generateNewLine()

            generateControlFlowCode("return@transaction when (id) ", endFlowString = "}\n") {
                collectCodeLineTemplates {
                    listOf(
                        CodeTemplate("null -> %L.new { storeValues() }", entitySimpleName),
                        CodeTemplate("else -> %L.new(id) { storeValues() }", entitySimpleName),
                    )
                }
            }
        }
    }
}

private fun createAssignAccessorValuesCodeBlock(accessorClassInfo: AccessorClassInfo): CodeTemplate =
    CodeTemplate {
        collectCodeLineTemplates {
            accessorClassInfo.petalValueColumns
                .map { column ->
                    CodeTemplate("this.%L = %L", column.name, column.name)
                }
        }

        collectCodeLineTemplates {
            accessorClassInfo.petalReferenceColumns
                .map { column ->
                    val name = column.name + column.getNullabilityExtension()
                    CodeTemplate("this.%L = %L", column.name, "$name.dbEntity")
                }
        }
    }
