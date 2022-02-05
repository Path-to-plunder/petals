package com.casadetasha.kexp.petals.processor.util

import com.casadetasha.kexp.petals.processor.PetalProcessor
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile

internal fun compileSources(vararg sourceFiles: SourceFile): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles.toList()
        annotationProcessors = listOf(PetalProcessor())
        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()
}
