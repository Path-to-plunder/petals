package com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions

import com.casadetasha.kexp.petals.processor.classgenerator.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorExportFunSpecBuilder {

    companion object {
        const val EXPORT_METHOD_SIMPLE_NAME = "export"
    }

    fun getFunSpec(accessorClassInfo: AccessorClassInfo): FunSpec {
        val statementParser = AccessorKtxFunctionParser(accessorClassInfo)
        return FunSpec.builder(EXPORT_METHOD_SIMPLE_NAME)
            .returns(accessorClassInfo.className)
            .receiver(accessorClassInfo.sourceClassName)
            .addCode(statementParser.exportMethodStatement)
            .build()
    }

    private class AccessorKtxFunctionParser(private val accessorClassInfo: AccessorClassInfo) {
        private val stringBuilder = StringBuilder()

        private val methodBody: String by lazy {
            stringBuilder.append("return ${accessorClassInfo.className.simpleName}(")
            amendSettersForColumns()
            closeExportCreation()
            stringBuilder.toString()
        }

        val exportMethodStatement: CodeBlock by lazy {
            return@lazy CodeBlock.builder()
                .addStatement(methodBody)
                .build()
        }

        private fun amendSettersForColumns(): StringBuilder {
            accessorClassInfo.columns
                .filterNot { it.isId }
                .forEach {
                    val constructorBlock = "\n  ${it.name} = ${it.name},"
                    stringBuilder.append(constructorBlock)
                }
            accessorClassInfo.columns
                .filter { it.isId }
                .forEach {
                    val constructorBlock = "\n  ${it.name} = ${it.name}.value,"
                    stringBuilder.append(constructorBlock)
                }
            return stringBuilder
        }

        private fun closeExportCreation(): StringBuilder {
            stringBuilder.removeTrailingComma()
            stringBuilder.append("\n).apply { isStored = true }")
            return stringBuilder
        }

        private fun StringBuilder.removeTrailingComma() : StringBuilder {
            val index = lastIndexOf(",")
            if (index == lastIndex) {
                deleteCharAt(index)
            }
            return this
        }
    }
}
