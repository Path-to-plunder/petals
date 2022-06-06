package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate.Companion.createFileTemplate
import com.casadetasha.kexp.generationdsl.dsl.ImportTemplate.Companion.importTemplate
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo

internal class AccessorClassFileGenerator(
    private val accessorClassInfo: AccessorClassInfo
) {

    fun generateFile() {
        createFileTemplate(
            directory = kaptKotlinGeneratedDir,
            packageName = PACKAGE_NAME,
            fileName = accessorClassInfo.className.simpleName
        ) {
            importTemplate(importPackage = "org.jetbrains.exposed.dao", importName = "load")

            accessorClassTemplate(accessorClassInfo)
        }.writeToDisk()
    }

    companion object {
        const val PACKAGE_NAME = "com.casadetasha.kexp.petals.accessor"
    }
}