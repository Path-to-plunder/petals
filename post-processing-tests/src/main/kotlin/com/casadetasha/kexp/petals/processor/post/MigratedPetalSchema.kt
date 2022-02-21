package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.VarChar
import java.util.*

@Petal(tableName = "migrated_petal", className = "MigratedPetal", version = 1, primaryKeyType = PetalPrimaryKey.INT)
interface MigratedPetalSchemaV1 {
    val checkingInt: Int
    val checkingLong: Long
    val checkingString: String
    @VarChar(charLimit = 10) val checkingVarChar: String
    val checkingUUID: UUID
}

@Petal(tableName = "migrated_petal", className = "MigratedPetal", version = 2, primaryKeyType = PetalPrimaryKey.INT)
interface MigratedPetalSchemaV2 {
    val uuid: UUID
    val color: String
    @VarChar(charLimit = 10) val secondColor: String
    val count: Int
    val sporeCount: Long
}

@Petal(tableName = "migrated_petal", className = "MigratedPetal", version = 3, primaryKeyType = PetalPrimaryKey.INT)
abstract class MigratedPetalSchemaV3 {
    @AlterColumn(renameFrom = "uuid") abstract val renamed_uuid: UUID
    @AlterColumn(renameFrom = "color") abstract val renamed_color: String
    @AlterColumn(renameFrom = "secondColor") @VarChar(charLimit = 10) abstract val renamed_secondColor: String
    @AlterColumn(renameFrom = "count") abstract val renamed_count: Int
    @AlterColumn(renameFrom = "sporeCount") abstract val renamed_sporeCount: Long
}
