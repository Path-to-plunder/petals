package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FileTemplate.Companion.fileTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ImportTemplate.Companion.importTemplate
import com.squareup.kotlinpoet.FileSpec
import java.io.File

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
//        val fileSpec = FileSpec.builder(
//            packageName = PACKAGE_NAME,
//            fileName = accessorClassInfo.className.simpleName
//        )
//            .addType(
//                AccessorClassSpecBuilder(
//                    accessorClassInfo
//                ).getClassSpec())
//            .addImport("org.jetbrains.exposed.dao", "load")
//            .build()
//
//        fileSpec.writeTo(File(kaptKotlinGeneratedDir))
    }

    companion object {
        const val PACKAGE_NAME = "com.casadetasha.kexp.petals.accessor"
    }
}