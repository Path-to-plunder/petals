package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(tableName = "no_id_petal", primaryKeyType = PetalPrimaryKey.NONE)
interface NoIdPetalSchema {
    val column: String
}

@Petal(tableName = "int_autoincrement_id_petal", primaryKeyType = PetalPrimaryKey.INT_AUTO_INCREMENT)
interface IntAutoincrementIdPetalSchema {
    val column: String
}

@Petal(tableName = "int_id_petal", primaryKeyType = PetalPrimaryKey.INT)
interface IntIdPetalSchema {
    val column: String
}

@Petal(tableName = "text_id_petal", primaryKeyType = PetalPrimaryKey.TEXT)
interface TextIdPetalSchema {
    val column: String
}

@Petal(tableName = "uuid_id_petal", primaryKeyType = PetalPrimaryKey.UUID)
interface UuidIdPetalSchema {
    val column: String
}
