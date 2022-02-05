package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(tableName = "long_id_petal", className = "LongIdPetal", primaryKeyType = PetalPrimaryKey.LONG)
interface LongIdPetalSchema {
    val column: String
}
