package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate.Companion.fileTemplate
import com.casadetasha.kexp.generationdsl.dsl.ImportTemplate.Companion.importTemplate

internal class AccessorClassFileGenerator(
    private val accessorClassInfo: AccessorClassInfo
) {

    fun generateFile() {
        fileTemplate(
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