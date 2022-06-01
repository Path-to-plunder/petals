package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.ExportMethodNames.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate

internal fun createExportFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(
        name = EXPORT_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className,
        receiverType = accessorClassInfo.entityClassName
    ) {
        this.methodBody {
            createExportFunctionBody(accessorClassInfo)
        }
}

private fun createExportFunctionBody(accessorClassInfo: AccessorClassInfo): CodeTemplate =
    CodeTemplate {
        code {  "return ${accessorClassInfo.className.simpleName}(" }
        code { "\n  dbEntity = this," }
        amendSettersForColumns(accessorClassInfo)
        code { "\n)" }
    }

private fun CodeTemplate.amendSettersForColumns(accessorClassInfo: AccessorClassInfo) = apply {
    accessorClassInfo.petalColumns
        .filterIsInstance<PetalValueColumn>()
        .forEach {
            code { "\n  ${it.name} = ${it.name}," }
        }

    accessorClassInfo.petalColumns
        .filterIsInstance<PetalReferenceColumn>()
        .forEach {
            val nullabilityState = if (it.isNullable) { "?" } else { "" }
            val constructorBlock = "\n  ${it.name}Id = readValues[%M.${it.name}]$nullabilityState.value,"
            codeTemplate { CodeTemplate(constructorBlock, accessorClassInfo.tableMemberName) }
        }

    accessorClassInfo.petalColumns
        .filterIsInstance<PetalIdColumn>()
        .forEach {
            code { "\n  ${it.name} = ${it.name}.value," }
        }
}

object ExportMethodNames {
    const val EXPORT_METHOD_SIMPLE_NAME = "toPetal"
}
