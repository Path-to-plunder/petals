package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.VarChar
import java.util.*

@Petal(tableName = "data_petal", className = "DataPetal", primaryKeyType = PetalPrimaryKey.UUID)
interface DataPetal

@PetalSchema(petal = DataPetal::class)
interface DataPetalSchemaV1 {
    val uuid: UUID
    var color: String?
    @VarChar(charLimit = 10) var secondColor: String
    var count: Int
    var sporeCount: Long
}
