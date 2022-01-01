package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(tableName = "no_id_petal", primaryKeyType = PetalPrimaryKey.NONE)
interface NoIdPetalSchema {
    val column: String
}

@Petal(tableName = "int_id_petal", primaryKeyType = PetalPrimaryKey.INT)
interface IntIdPetalSchema {
    val column: String
}

@Petal(tableName = "long_id_petal", primaryKeyType = PetalPrimaryKey.LONG)
interface LongIdPetalSchema {
    val column: String
}

@Petal(tableName = "uuid_id_petal", primaryKeyType = PetalPrimaryKey.UUID)
interface UuidIdPetalSchema {
    val column: String
}
