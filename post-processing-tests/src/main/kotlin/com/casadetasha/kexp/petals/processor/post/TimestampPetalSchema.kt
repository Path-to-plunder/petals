package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.IncludeTimestamps
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(
    tableName = "timestamp_petal",
    className = "TimestampPetalData",
    primaryKeyType = PetalPrimaryKey.INT)
interface TimestampPetal

@PetalSchema(petal = TimestampPetal::class)
@IncludeTimestamps
interface TimestampPetalSchema {
    val column: String
}
