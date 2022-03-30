package com.casadetasha.kexp.petals.processor.inputparser

import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.Petal
import com.squareup.kotlinpoet.ClassName

internal open class ParsedSchemalessPetal protected constructor(
    val kotlinClass: KotlinContainer.KotlinClass,
    val petalAnnotation: Petal
) {

    val className: ClassName = kotlinClass.className

    companion object {
        fun parseFromKotlinClass(kotlinClass: KotlinContainer.KotlinClass): ParsedSchemalessPetal {
            return ParsedSchemalessPetal(
                kotlinClass = kotlinClass,
                petalAnnotation = kotlinClass.getAnnotation(Petal::class)!!
            )
        }
    }
}