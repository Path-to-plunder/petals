package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(tableName = "uuid_id_petal", className = "UuidIdPetal", primaryKeyType = PetalPrimaryKey.UUID)
interface UuidIdPetalSchema {
    val column: String
}
