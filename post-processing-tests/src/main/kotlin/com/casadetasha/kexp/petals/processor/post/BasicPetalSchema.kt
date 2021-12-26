package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import java.util.*

import kotlin.Int

@Petal(tableName = "basic_petal", version = 1)
interface BasicPetalSchemaV1 {
    val checkingInt: Int
    val checkingLong: Long
    val checkingString: String
    val checkingUUID: UUID
}

@Petal(tableName = "basic_petal", version = 2)
interface BasicPetalSchemaV2 {
    val uuid: UUID
    val color: String
    val count: Int
    val sporeCount: Long
}
