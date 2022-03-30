package com.casadetasha.kexp.petals.processor.inputparser

import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.squareup.kotlinpoet.ClassName

internal class CleanPetalAnnotationParser(private val petalClasses: PetalClasses) {

    fun parsePetalMap(): Map<ClassName, ParsedPetal> {
        val parsedSchemalessPetals: Map<ClassName, ParsedSchemalessPetal> = petalClasses.PETAL_CLASSES
            .mapValues { (_, petalClass) -> ParsedSchemalessPetal.parseFromKotlinClass(petalClass) }

        return petalClasses.SCHEMA_CLASSES
            .map { ParsedPetalSchema.parseFromAnnotatedSchemaClass(parsedSchemalessPetals, it) }
            .groupBy { it.parsedSchemalessPetal }
            .map { (petal, schemaList) ->
                petal.className to ParsedPetal(
                    kotlinClass = petal.kotlinClass,
                    petalAnnotation = petal.petalAnnotation,
                    schemas = schemaList.associateBy { it.schemaVersion }.toSortedMap())
            }.toMap()
    }
}
