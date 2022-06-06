package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal object DslDataClassFileGenerator {

    fun generateFile(accessorClassInfo: AccessorClassInfo) {
        FileTemplate.generateFile(
            directory = AnnotationParser.kaptKotlinGeneratedDir,
            packageName = DataClassTemplateValues.PACKAGE_NAME,
            fileName = accessorClassInfo.dataClassName.simpleName
        ) {
            createDataClassFromTemplate(accessorClassInfo)
        }.writeToDisk()
    }
}
