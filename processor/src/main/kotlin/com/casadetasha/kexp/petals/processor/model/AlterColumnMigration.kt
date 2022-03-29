package com.casadetasha.kexp.petals.processor.model

import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalColumn

internal class AlterColumnMigration(
    val previousColumnState: UnprocessedPetalColumn,
    val updatedColumnState: UnprocessedPetalColumn
)
