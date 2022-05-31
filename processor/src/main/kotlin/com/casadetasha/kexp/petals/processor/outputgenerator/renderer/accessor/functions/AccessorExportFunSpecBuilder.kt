package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.ExportMethodNames.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate
import com.squareup.kotlinpoet.CodeBlock

internal fun createExportFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(
        name = EXPORT_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className,
        receiverType = accessorClassInfo.entityClassName
    ) {
        writeCode {
            createExportFunctionBody(accessorClassInfo)
        }
}

private fun createExportFunctionBody(accessorClassInfo: AccessorClassInfo): CodeTemplate =
    CodeTemplate(
        CodeBlock.builder().add("return ${accessorClassInfo.className.simpleName}(")
            .add("\n  dbEntity = this,")
            .amendSettersForColumns(accessorClassInfo)
            .add("\n)")
            .build()
    )

private fun CodeBlock.Builder.amendSettersForColumns(accessorClassInfo: AccessorClassInfo) = apply {
    accessorClassInfo.petalColumns
        .filterIsInstance<PetalValueColumn>()
        .forEach {
            val constructorBlock = "\n  ${it.name} = ${it.name},"
            add(constructorBlock)
        }
    accessorClassInfo.petalColumns
        .filterIsInstance<PetalReferenceColumn>()
        .forEach {
            val nullabilityState = if (it.isNullable) { "?" } else { "" }
            val constructorBlock = "\n  ${it.name}Id = readValues[%M.${it.name}]$nullabilityState.value,"
            add(constructorBlock, accessorClassInfo.tableMemberName)
        }
    accessorClassInfo.petalColumns
        .filterIsInstance<PetalIdColumn>()
        .forEach {
            val constructorBlock = "\n  ${it.name} = ${it.name}.value,"
            add(constructorBlock)
        }
}

object ExportMethodNames {
    const val EXPORT_METHOD_SIMPLE_NAME = "toPetal"
}
