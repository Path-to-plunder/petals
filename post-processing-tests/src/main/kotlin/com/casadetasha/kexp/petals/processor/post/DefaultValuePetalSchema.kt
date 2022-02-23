package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.*
import java.util.*

@Petal(tableName = "default_value_petal", className = "DefaultValuePetal", primaryKeyType = PetalPrimaryKey.UUID)
interface DefaultValuePetalSchemaV1 {
    val uuid: UUID
    @DefaultString("default color") val color: String
    @DefaultNull @VarChar(charLimit = 10) val secondColor: String?
    @DefaultInt(10) val count: Int
    @DefaultLong(200) val sporeCount: Long
}
