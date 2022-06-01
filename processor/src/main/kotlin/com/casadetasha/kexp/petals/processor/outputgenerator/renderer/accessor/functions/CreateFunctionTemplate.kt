package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.CreateMethodNames.CREATE_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.ExportMethodNames.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate.Companion.collectParameterTemplates
import com.squareup.kotlinpoet.*

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
    val entityMemberName = accessorClassInfo.entityMemberName
    return CodeTemplate {
        controlFlowCode(
            prefix = "return %M", TRANSACTION_MEMBER_NAME,
            suffix = ".$EXPORT_METHOD_SIMPLE_NAME()"
        ) {
            controlFlowCode("val storeValues: %M.() -> Unit = ", entityMemberName) {
                codeTemplate { createAssignAccessorValuesCodeBlock(accessorClassInfo) }
            }

            controlFlowCode("return@transaction when (id) ") {
                collectCodeLineTemplates {
                    listOf(
                        CodeTemplate("null -> %M.new { storeValues() }", entityMemberName),
                        CodeTemplate("else -> %M.new(id) { storeValues() }", entityMemberName),
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

internal fun PetalReferenceColumn.getNullabilityExtension(): Any {
    return if (isNullable) {
        "?"
    } else {
        ""
    }
}

internal object CreateMethodNames {
    const val CREATE_METHOD_SIMPLE_NAME = "create"

    val TRANSACTION_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
}
