package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.VarChar
import java.util.*

@Petal(tableName = "basic_petal", className = "BasicPetal", primaryKeyType = PetalPrimaryKey.UUID)
interface BasicPetalSchemaV1 {
    val uuid: UUID
    val color: String?
    @VarChar(charLimit = 10) val secondColor: String
    val count: Int
    val sporeCount: Long
}
