package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.CREATE_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate.Companion.collectParameterTemplates
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

        this.methodBody {
            createCreateFunctionMethodBodyTemplate(accessorClassInfo)
        }
    }


private fun createCreateFunctionMethodBodyTemplate(accessorClassInfo: AccessorClassInfo): CodeTemplate {
    val entitySimpleName = accessorClassInfo.entityClassName.simpleName
    return CodeTemplate {
        controlFlowCode(
            prefix = "return %M", TRANSACTION_MEMBER_NAME,
            suffix = ".$EXPORT_PETAL_METHOD_SIMPLE_NAME()"
        ) {
            controlFlowCode("val storeValues: %L.() -> Unit = ", entitySimpleName) {
                codeTemplate { createAssignAccessorValuesCodeBlock(accessorClassInfo) }
            }

            controlFlowCode("return@transaction when (id) ") {
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
