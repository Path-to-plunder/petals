package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.FileSpec
import java.io.File

class FileTemplate private constructor(
    private val directory: String,
    packageName: String,
    fileName: String,
    buildFileFunction: FileTemplate.() -> Unit): KotlinContainerTemplate {

    private val fileBuilder = FileSpec.builder(
        packageName = packageName,
        fileName = fileName
    )

    private val fileSpec by lazy { fileBuilder.build() }

    init {
        this.buildFileFunction()
    }

    fun writeToDisk() {
        fileSpec.writeTo(File(directory))
    }

    internal fun addClass(classTemplate: ClassTemplate) {
        fileBuilder.addType(classTemplate.classSpec)
    }

    override fun addFunction(functionTemplate: FunctionTemplate) {
        fileBuilder.addFunction(functionTemplate.functionSpec)
    }

    override fun addProperties(properties: Collection<PropertyTemplate>) {
        properties.forEach{ fileBuilder.addProperty(it.propertySpec) }
    }

    companion object {
        fun fileTemplate(directory: String, packageName: String, fileName: String, buildFileFunction: FileTemplate.() -> Unit,): FileTemplate =
            FileTemplate(directory, packageName, fileName, buildFileFunction)
    }
}
