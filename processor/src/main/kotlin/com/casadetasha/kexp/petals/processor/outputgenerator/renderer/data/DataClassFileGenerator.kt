package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import java.io.File

@OptIn(KotlinPoetMetadataPreview::class)
internal class DataClassFileGenerator(
    private val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
) {

    fun generateFile() {
        val fileSpec = FileSpec.builder(
            packageName = PACKAGE_NAME,
            fileName = accessorClassInfo.className.simpleName + "Data"
        )
            .addType(DataClassSpecBuilder(accessorClassInfo).getClassSpec())
            .addFunction(DataExportFunSpecBuilder(accessorClassInfo).entityExportFunSpec)
            .addFunction(DataExportFunSpecBuilder(accessorClassInfo).accessorExportFunSpec)
            .build()

        fileSpec.writeTo(File(kaptKotlinGeneratedDir))
    }

    companion object {
        const val PACKAGE_NAME = "com.casadetasha.kexp.petals.data"
    }
}