package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.AlterColumn
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

@Petal(tableName = "basic_petal", version = 3)
abstract class BasicPetalSchemaV3 {
    @AlterColumn(previousName = "uuid") abstract val renamed_uuid: UUID
    @AlterColumn(previousName = "color") abstract val renamed_color: String
    @AlterColumn(previousName = "count") abstract val renamed_count: Int
    @AlterColumn(previousName = "sporeCount") abstract val renamed_sporeCount: Long
}
