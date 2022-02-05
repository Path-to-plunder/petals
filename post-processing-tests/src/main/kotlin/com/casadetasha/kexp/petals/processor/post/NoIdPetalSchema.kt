package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(tableName = "no_id_petal", className = "NoIdPetal", primaryKeyType = PetalPrimaryKey.NONE)
interface NoIdPetalSchema {
    val column: String
}
