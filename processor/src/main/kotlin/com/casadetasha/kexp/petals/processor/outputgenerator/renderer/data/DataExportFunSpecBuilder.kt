package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data

import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class DataExportFunSpecBuilder(private val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

    private val codeBuilder = CodeBlock.builder()

    val entityExportFunSpec by lazy {
        FunSpec.builder(EXPORT_METHOD_SIMPLE_NAME)
            .receiver(accessorClassInfo.entityClassName)
            .returns(accessorClassInfo.dataClassName)
            .addCode(entityExportMethodStatement)
            .build()
    }

    val accessorExportFunSpec by lazy {
        FunSpec.builder(EXPORT_METHOD_SIMPLE_NAME)
            .receiver(accessorClassInfo.className)
            .returns(accessorClassInfo.dataClassName)
            .addCode(accessorExportMethodStatement)
            .build()
    }

    private val entityExportMethodStatement: CodeBlock by lazy {
        codeBuilder.add("return ${accessorClassInfo.dataClassName.simpleName}(")
        amendSettersForEntityColumns()
        closeExportCreation()

        return@lazy codeBuilder.build()
    }

    private val accessorExportMethodStatement: CodeBlock by lazy {
        codeBuilder.add("return ${accessorClassInfo.dataClassName.simpleName}(")
        amendSettersForAccessorColumns()
        closeExportCreation()

        return@lazy codeBuilder.build()
    }

    private fun amendSettersForEntityColumns() {
        val entityColumns = accessorClassInfo.petalColumns
            .filterIsInstance<LocalPetalColumn>()
            .filterNot { it is PetalIdColumn }

        entityColumns.filterIsInstance<PetalValueColumn>()
            .forEach {
                val constructorBlock = "\n  ${it.name} = ${it.name},"
                codeBuilder.add(constructorBlock)
            }

        entityColumns
            .filterIsInstance<PetalReferenceColumn>()
            .forEach {
                val nullibleState = if (it.isNullable) { "?" } else { "" }
                val constructorBlock = "\n  ${it.name}Id = readValues[%M.${it.name}]$nullibleState.value,"
                codeBuilder.add(constructorBlock, accessorClassInfo.tableMemberName)
            }

        accessorClassInfo.petalColumns
            .filterIsInstance<PetalIdColumn>()
            .forEach {
                val constructorBlock = "\n  ${it.name} = ${it.name}.value,"
                codeBuilder.add(constructorBlock)
            }
    }

    private fun amendSettersForAccessorColumns() {
        accessorClassInfo.petalColumns
            .filterIsInstance<LocalPetalColumn>()
            .map {
                when (it) {
                    is PetalReferenceColumn -> "${it.name}Id"
                    else -> it.name
                }
            }
            .forEach {
                val constructorBlock = "\n  $it = $it,"
                codeBuilder.add(constructorBlock)
            }
    }

    private fun closeExportCreation() {
        codeBuilder.add("\n)")
    }

    companion object {
        const val EXPORT_METHOD_SIMPLE_NAME = "asData"
    }
}
