package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.VarChar
import java.util.*

import kotlin.Int

@Petal(tableName = "basic_petal", version = 1)
interface BasicPetalSchemaV1 {
    val checkingInt: Int
    val checkingLong: Long
    val checkingString: String
    @VarChar val checkingVarChar: String
    @VarChar(charLimit = 10) val checkingCappedVarChar: String
    val checkingUUID: UUID
}

@Petal(tableName = "basic_petal", version = 2)
interface BasicPetalSchemaV2 {
    val uuid: UUID
    val color: String
    @VarChar val secondColor: String
    @VarChar(charLimit = 10) val thirdColor: String
    val count: Int
    val sporeCount: Long
}

@Petal(tableName = "basic_petal", version = 3)
abstract class BasicPetalSchemaV3 {
    @AlterColumn(renameFrom = "uuid") abstract val renamed_uuid: UUID
    @AlterColumn(renameFrom = "color") abstract val renamed_color: String
    @AlterColumn(renameFrom = "secondColor") @VarChar abstract val renamed_secondColor: String
    @AlterColumn(renameFrom = "thirdColor") @VarChar(charLimit = 10) abstract val renamed_thirdColor: String
    @AlterColumn(renameFrom = "count") abstract val renamed_count: Int
    @AlterColumn(renameFrom = "sporeCount") abstract val renamed_sporeCount: Long
}
