package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import java.util.*

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorFunSpecBuilder() {

    companion object {
        const val EXPORT_METHOD_SIMPLE_NAME = "export";
    }

    fun getFunSpec(accessorClassInfo: AccessorClassInfo): FunSpec {
        val statementParser = AccessorKtxFunctionParser(accessorClassInfo)
        return FunSpec.builder(EXPORT_METHOD_SIMPLE_NAME)
            .returns(accessorClassInfo.className)
            .receiver(accessorClassInfo.sourceClassName)
            .addStatement(statementParser.exportMethodStatement)
            .build()
    }

    private class AccessorKtxFunctionParser(private val accessorClassInfo: AccessorClassInfo) {
        private val stringBuilder = StringBuilder()

        val exportMethodStatement: String by lazy {
            stringBuilder.append("return ${accessorClassInfo.className.simpleName}(")
            amendSettersForColumns()
            closeExportCreation()
            stringBuilder.toString()
        }

        private fun amendSettersForColumns(): StringBuilder {
            accessorClassInfo.columns
                .filterNot { it.isId!! }
                .forEach {
                    val constructorBlock = "\n  ${it.name} = ${it.name},"
                    stringBuilder.append(constructorBlock)
                }
            accessorClassInfo.columns
                .filter { it.isId!! }
                .forEach {
                    val constructorBlock = "\n  ${it.name} = ${it.name}.value,"
                    stringBuilder.append(constructorBlock)
                }
            return stringBuilder
        }

        private fun closeExportCreation(): StringBuilder {
            stringBuilder.removeTrailingComma()
            stringBuilder.append("\n)")
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
