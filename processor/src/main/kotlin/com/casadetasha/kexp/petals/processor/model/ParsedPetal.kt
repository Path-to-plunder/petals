package com.casadetasha.kexp.petals.processor.model

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
) {

    val currentSchema: ParsedPetalSchema? = schemas.values.lastOrNull()
}