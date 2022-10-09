package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.*
import java.util.*

@Petal(tableName = "default_value_petal", className = "DefaultValuePetal", primaryKeyType = PetalPrimaryKey.UUID)
interface DefaultValuePetal

@PetalSchema(petal = DefaultValuePetal::class)
interface DefaultValuePetalSchema {
    @DefaultString("default string value") val stringValue: String
    @DefaultInt(10) val intValue: Int
    @DefaultLong(200) val longValue: Long
}
