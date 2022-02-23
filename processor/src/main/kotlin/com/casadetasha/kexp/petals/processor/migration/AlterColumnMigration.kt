package com.casadetasha.kexp.petals.processor.migration

import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn

internal class AlterColumnMigration(
    val previousColumnState: UnprocessedPetalColumn,
    val updatedColumnState: UnprocessedPetalColumn
)
