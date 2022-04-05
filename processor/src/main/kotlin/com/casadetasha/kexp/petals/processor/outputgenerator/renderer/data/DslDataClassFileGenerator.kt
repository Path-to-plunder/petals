package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data

import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal object DslDataClassFileGenerator {

    fun generateFile(accessorClassInfo: AccessorClassInfo) {
        createDataClassFromTemplate(accessorClassInfo).writeToDisk()
    }
}
