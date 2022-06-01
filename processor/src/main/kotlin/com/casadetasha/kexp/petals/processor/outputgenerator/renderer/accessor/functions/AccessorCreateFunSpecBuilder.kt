package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.CreateMethodNames.CREATE_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.ExportMethodNames.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate.Companion.collectParameterTemplates
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
        controlFlow(
            prefix = "return %M", TRANSACTION_MEMBER_NAME,
            suffix = ".$EXPORT_METHOD_SIMPLE_NAME()"
        ) {
            controlFlow("val storeValues: %M.() -> Unit = ", entityMemberName) {
                codeTemplate { createAssignAccessorValuesCodeBlock(accessorClassInfo) }
            }

            controlFlow("return@transaction when (id) ") {
                collectStatementTemplates {
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
        collectStatementTemplates {
            accessorClassInfo.petalValueColumns
                .map { column ->
                    CodeTemplate("this.%L = %L", column.name, column.name)
                }
        }

        collectStatementTemplates {
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
