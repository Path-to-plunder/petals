package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(tableName = "int_id_petal", className = "IntIdPetal", primaryKeyType = PetalPrimaryKey.INT)
interface IntIdPetal

@PetalSchema(petal = IntIdPetal::class)
interface IntIdPetalSchema {
    val column: String
}
