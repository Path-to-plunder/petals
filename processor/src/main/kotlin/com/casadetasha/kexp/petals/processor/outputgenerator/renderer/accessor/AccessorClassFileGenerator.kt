package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate.Companion.generateFile
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions.createExportFunctionTemplate

internal class AccessorClassFileGenerator(
    private val accessorClassInfo: AccessorClassInfo
) {

    fun generateFile() =
        generateFile(
            directory = kaptKotlinGeneratedDir,
            packageName = PACKAGE_NAME,
            fileName = accessorClassInfo.className.simpleName
        ) {
            generateImport(importPackage = "org.jetbrains.exposed.dao", importName = "load")

            generateAccessorClass(accessorClassInfo)

            collectFunctionTemplates {
                listOf(createExportFunctionTemplate(accessorClassInfo))
            }
        }.writeToDisk()

    companion object {
        const val PACKAGE_NAME = "com.casadetasha.kexp.petals.accessor"
    }
}