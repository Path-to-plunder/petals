package com.casadetasha.kexp.petals.processor.migration

import com.casadetasha.kexp.petals.annotations.PetalColumn

class AlterColumnMigration(val previousColumnState: PetalColumn,
                           val updatedColumnState: PetalColumn
)
