package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.IncludeTimestamps
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey

@Petal(tableName = "one_hour_expiration_petal", className = "OneHourExpiration", primaryKeyType = PetalPrimaryKey.LONG)
interface OneHourExpirationPetal

@PetalSchema(petal = OneHourExpirationPetal::class)
@IncludeTimestamps
interface OneHourExpirationPetalSchema {
    val column: String
}
