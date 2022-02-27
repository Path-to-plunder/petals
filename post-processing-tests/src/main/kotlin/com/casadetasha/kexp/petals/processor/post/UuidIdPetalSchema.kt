package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(tableName = "uuid_id_petal", className = "UuidIdPetal", primaryKeyType = PetalPrimaryKey.UUID)
interface UuidIdPetal

@PetalSchema(petal = UuidIdPetal::class)
interface UuidIdPetalSchema {
    val column: String
}
