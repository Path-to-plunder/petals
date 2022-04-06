package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.*
import java.util.*

@Petal(tableName = "default_value_petal", className = "DefaultValuePetal", primaryKeyType = PetalPrimaryKey.UUID)
interface DefaultValuePetal

@PetalSchema(petal = DefaultValuePetal::class)
interface DefaultValuePetalSchemaV1 {
    val uuid: UUID
    @DefaultString("default color") val color: String
    @DefaultString("will not be a default color") val startingDefaultColor: String
    val endingDefaultColor: String
    @VarChar(charLimit = 10) val secondColor: String?
    @DefaultInt(10) val count: Int
    @DefaultLong(200) val sporeCount: Long
}

@PetalSchema(petal = DefaultValuePetal::class, version = 2)
interface DefaultValuePetalSchemaV2 {
    val uuid: UUID
    @DefaultString("default color") val color: String
    @AlterColumn val startingDefaultColor: String
    @AlterColumn @DefaultString("different default color") val endingDefaultColor: String
    @VarChar(charLimit = 10) val secondColor: String?
    @DefaultInt(10) val count: Int
    @DefaultLong(200) val sporeCount: Long
}
