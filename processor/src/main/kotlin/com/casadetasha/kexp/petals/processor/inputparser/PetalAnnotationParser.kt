package com.casadetasha.kexp.petals.processor.inputparser

import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.casadetasha.kexp.petals.processor.model.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.model.ParsedSchemalessPetal
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.squareup.kotlinpoet.ClassName

internal object PetalAnnotationParser {

    fun parsePetalClasses(): PetalClasses {
        val petalClasses = PetalClasses()
        val parsedSchemalessPetals: Map<ClassName, ParsedSchemalessPetal> = petalClasses.petalClasses
            .mapValues { (_, petalClass) -> ParsedSchemalessPetal.parseFromKotlinClass(petalClass) }

        petalClasses.petalMap = petalClasses.schemaClasses
            .map { ParsedPetalSchema.parseFromAnnotatedSchemaClass(parsedSchemalessPetals, it) }
            .groupBy { it.parsedSchemalessPetal }
            .map { (petal, schemaList) ->
                petal.className to ParsedPetal(
                    kotlinClass = petal.kotlinClass,
                    petalAnnotation = petal.petalAnnotation,
                    schemas = schemaList.associateBy { it.schemaVersion }.toSortedMap())
            }.toMap()

        petalClasses.schemaMap = petalClasses.petalMap
            .filter { (_, parsedPetal) -> parsedPetal.currentSchema != null }
            .mapValues { (_, parsedPetal) -> parsedPetal.currentSchema!! }

        return petalClasses
    }
}
