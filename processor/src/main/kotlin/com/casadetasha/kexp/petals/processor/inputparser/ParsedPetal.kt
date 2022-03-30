package com.casadetasha.kexp.petals.processor.inputparser

import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.Petal
import java.util.*

internal class ParsedPetal(
    kotlinClass: KotlinContainer.KotlinClass,
    petalAnnotation: Petal,
    val schemas: SortedMap<Int, ParsedPetalSchema>
): ParsedSchemalessPetal(
    kotlinClass = kotlinClass,
    petalAnnotation = petalAnnotation,
)