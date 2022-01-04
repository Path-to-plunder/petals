package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.kexportable.annotations.PetalColumn

class AlterColumnMigration(val previousColumnState: PetalColumn,
                           val updatedColumnState: PetalColumn)
