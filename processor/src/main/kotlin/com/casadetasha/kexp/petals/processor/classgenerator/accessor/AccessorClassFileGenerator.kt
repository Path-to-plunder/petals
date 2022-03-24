package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import java.io.File

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorClassFileGenerator(
    private val accessorClassInfo: AccessorClassInfo
) {

    fun generateFile() {
        val fileSpec = FileSpec.builder(
            packageName = PACKAGE_NAME,
            fileName = accessorClassInfo.className.simpleName
        )
            .addType(AccessorClassSpecBuilder(accessorClassInfo).getClassSpec())
            .addImport("org.jetbrains.exposed.dao", "load")
            .build()

        fileSpec.writeTo(File(kaptKotlinGeneratedDir))
    }

    companion object {
        const val PACKAGE_NAME = "com.casadetasha.kexp.petals.accessor"
    }
}