package com.casadetasha.kexp.petals.processor.inputparser

import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalMigration
import java.util.*

internal class ParsedPetal(
    kotlinClass: KotlinContainer.KotlinClass,
    petalAnnotation: Petal,
    val schemas: SortedMap<Int, ParsedPetalSchema>
): ParsedSchemalessPetal(
    kotlinClass = kotlinClass,
    petalAnnotation = petalAnnotation,
) {

    fun getCurrentSchema(): ParsedPetalSchema? {
        return schemas.values.lastOrNull()
    }

    fun processMigration(): PetalMigration {
        return PetalMigration(
            tableName = tableName,
            className = className.simpleName,
            schemaMigrations = schemas.mapValues { (_, schema) -> schema.processMigration() }
        )
    }
}