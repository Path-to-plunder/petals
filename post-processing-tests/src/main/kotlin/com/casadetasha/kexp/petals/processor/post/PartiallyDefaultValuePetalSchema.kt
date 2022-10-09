package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.*
import java.util.*

@Petal(tableName = "partially_default_value_petal", className = "PartiallyDefaultValuePetal", primaryKeyType = PetalPrimaryKey.UUID)
interface PartiallyDefaultValuePetal

@PetalSchema(petal = PartiallyDefaultValuePetal::class)
interface PartiallyDefaultValuePetalSchemaV1 {
    val uuid: UUID
    @DefaultString("default color") val color: String
    @DefaultString("will not be a default color") val startingDefaultColor: String
    val endingDefaultColor: String
    @VarChar(charLimit = 10) val secondColor: String?
    @DefaultInt(10) val count: Int
    @DefaultLong(200) val sporeCount: Long
}

@PetalSchema(petal = PartiallyDefaultValuePetal::class, version = 2)
interface PartiallyDefaultValuePetalSchemaV2 {
    val uuid: UUID
    @DefaultString("default color") val color: String
    @AlterColumn val startingDefaultColor: String
    @AlterColumn @DefaultString("different default color") val endingDefaultColor: String
    @VarChar(charLimit = 10) val secondColor: String?
    @DefaultInt(10) val count: Int
    @DefaultLong(200) val sporeCount: Long
}
