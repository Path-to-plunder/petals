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
            .map { ParsedPetalSchema.parseWithAnnotatedSchemaClass(parsedSchemalessPetals, it) }
            .groupBy { it.parsedSchemalessPetal }
            .map { (petal, schemaList) ->
                petal.className to ParsedPetal(
                    kotlinClass = petal.kotlinClass,
                    petalAnnotation = petal.petalAnnotation,
                    schemas = schemaList.associateBy { it.schemaVersion }.toSortedMap())
            }.toMap()

        checkNoDuplicatePetalNames(petalClasses.petalMap)

        petalClasses.schemaMap = petalClasses.petalMap
            .filter { (_, parsedPetal) -> parsedPetal.currentSchema != null }
            .mapValues { (_, parsedPetal) -> parsedPetal.currentSchema!! }

        return petalClasses
    }

    private fun checkNoDuplicatePetalNames(petalMap: Map<ClassName, ParsedPetal>) {
        checkNoDuplicateClassNames(petalMap)
        checkNoDuplicateTableNames(petalMap)
    }

    private fun checkNoDuplicateClassNames(petalMap: Map<ClassName, ParsedPetal>) {
        val duplicateClassNames = petalMap.entries
            .groupingBy { it.value.petalAnnotation.className }
            .eachCount()
            .filter { it.value > 1 }

        if (duplicateClassNames.isNotEmpty()) { throw IllegalStateException("Duplicate petal class names found. All petal class names must be unique. Duplicate class names: ${duplicateClassNames.keys.joinToString(", ")}") }
    }

    private fun checkNoDuplicateTableNames(petalMap: Map<ClassName, ParsedPetal>) {
        val duplicateClassNames = petalMap.entries
            .groupingBy { it.value.petalAnnotation.tableName }
            .eachCount()
            .filter { it.value > 1 }

        if (duplicateClassNames.isNotEmpty()) { throw IllegalStateException("Duplicate petal table names found. All petal table names must be unique. Duplicate table names: ${duplicateClassNames.keys.joinToString(", ")}") }
    }
}
