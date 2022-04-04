package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import java.io.File

class FileTemplate private constructor(directory: String, packageName: String, fileName: String, function: () -> Unit,) {

    fun writeToDisk() {
        TODO("Not yet implemented")
    }

    companion object {
        fun fileTemplate(directory: String, packageName: String, fileName: String, function: () -> Unit,): FileTemplate =
            FileTemplate(directory, packageName, fileName, function)
    }
}
