package com.casadetasha.kexp.petals.processor.classgenerator.data

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class DataExportFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

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
        val entityColumns = accessorClassInfo.columns
            .filterNot { it.isId }
            .filterNot { it.isReferencedByColumn }

        entityColumns.filterNot { it.isReferenceColumn }
            .forEach {
                val constructorBlock = "\n  ${it.name} = ${it.name},"
                codeBuilder.add(constructorBlock)
            }

        entityColumns
            .filter { it.isReferenceColumn }
            .forEach {
                val constructorBlock = "\n  ${it.name}Id = readValues[%M.${it.name}].value,"
                codeBuilder.add(constructorBlock, accessorClassInfo.tableMemberName)
            }

        accessorClassInfo.columns
            .filter { it.isId }
            .forEach {
                val constructorBlock = "\n  ${it.name} = ${it.name}.value,"
                codeBuilder.add(constructorBlock)
            }
    }

    private fun amendSettersForAccessorColumns() {
        accessorClassInfo.columns
            .filterNot { it.isReferencedByColumn }
            .map {
                when (it.isReferenceColumn) {
                    true -> "${it.name}Id"
                    false -> it.name
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
