package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import java.io.File

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorClassFileGenerator(
    private val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
) {

    fun generateFile() {
        val fileSpec = FileSpec.builder(
            packageName = com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassFileGenerator.Companion.PACKAGE_NAME,
            fileName = accessorClassInfo.className.simpleName
        )
            .addType(
                com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassSpecBuilder(
                    accessorClassInfo
                ).getClassSpec())
            .addImport("org.jetbrains.exposed.dao", "load")
            .build()

        fileSpec.writeTo(File(kaptKotlinGeneratedDir))
    }

    companion object {
        const val PACKAGE_NAME = "com.casadetasha.kexp.petals.accessor"
    }
}