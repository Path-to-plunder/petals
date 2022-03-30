package com.casadetasha.kexp.petals.processor.model

import com.casadetasha.kexp.petals.processor.inputparser.LocalPetalColumn

internal class AlterColumnMigration(
    val previousColumnSchema: LocalPetalColumn,
    val updatedColumnSchema: LocalPetalColumn
)
