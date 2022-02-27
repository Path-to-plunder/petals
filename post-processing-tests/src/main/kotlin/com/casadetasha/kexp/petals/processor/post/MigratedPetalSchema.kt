package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.*
import java.util.*

@Petal(tableName = "migrated_petal", className = "MigratedPetal", primaryKeyType = PetalPrimaryKey.INT)
interface MigratedPetal

@PetalSchema(petal = MigratedPetal::class, version = 1)
interface MigratedPetalSchemaV1 {
    val checkingInt: Int
    val checkingLong: Long
    val checkingString: String
    @VarChar(charLimit = 10) val checkingVarChar: String
    val checkingUUID: UUID
}

@PetalSchema(petal = MigratedPetal::class, version = 2)
interface MigratedPetalSchemaV2 {
    val uuid: UUID
    val color: String
    @VarChar(charLimit = 10) val secondColor: String
    val count: Int
    val sporeCount: Long
}

@PetalSchema(petal = MigratedPetal::class, version = 3)
abstract class MigratedPetalSchemaV3 {
    @AlterColumn(renameFrom = "uuid") abstract val renamed_uuid: UUID
    @AlterColumn(renameFrom = "color") abstract val renamed_color: String
    @AlterColumn(renameFrom = "secondColor") @VarChar(charLimit = 10) abstract val renamed_secondColor: String
    @AlterColumn(renameFrom = "count") abstract val renamed_count: Int
    @AlterColumn(renameFrom = "sporeCount") abstract val renamed_sporeCount: Long
}
