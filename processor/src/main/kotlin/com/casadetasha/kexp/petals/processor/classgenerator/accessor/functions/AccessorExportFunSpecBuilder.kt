package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorExportFunSpecBuilder(private val accessorClassInfo: AccessorClassInfo) {

    private val codeBuilder = CodeBlock.builder()

    val exportFunSpec by lazy {
        FunSpec.builder(EXPORT_METHOD_SIMPLE_NAME)
            .receiver(accessorClassInfo.entityClassName)
            .returns(accessorClassInfo.className)
            .addCode(exportMethodStatement)
            .build()
    }

    private val exportMethodStatement: CodeBlock by lazy {
        codeBuilder.add("return ${accessorClassInfo.className.simpleName}(")
        addEntity()
        amendSettersForColumns()
        closeExportCreation()

        return@lazy codeBuilder.build()
    }

    private fun addEntity() {
        codeBuilder.add("\n  dbEntity = this,")
    }

    private fun amendSettersForColumns() {
        accessorClassInfo.columns
            .filterNot { it.isId }
            .filter { it.columnReferenceInfo == null }
            .forEach {
                val constructorBlock = "\n  ${it.name} = ${it.name},"
                codeBuilder.add(constructorBlock)
            }
        accessorClassInfo.columns
            .filterNot { it.isId }
            .filter { it.columnReferenceInfo != null }
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

    private fun closeExportCreation() {
        codeBuilder.add("\n)")
    }

    companion object {
        const val EXPORT_METHOD_SIMPLE_NAME = "toPetal"
    }
}
