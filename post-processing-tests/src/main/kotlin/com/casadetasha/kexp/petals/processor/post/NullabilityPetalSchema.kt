package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal

@Petal(tableName = "starting_nullable_petal", version = 1)
interface StartingNullablePetalSchemaV1 {
    val color: String?
}

@Petal(tableName = "starting_nullable_petal", version = 2)
interface StartingNullablePetalSchemaV2 {
    @AlterColumn(previousName = "color") val color: String
}

@Petal(tableName = "starting_non_nullable_petal", version = 1)
interface StartingNonNullablePetalSchemaV1 {
    val color: String
}

@Petal(tableName = "starting_non_nullable_petal", version = 2)
interface StartingNonNullablePetalSchemaV2 {
    @AlterColumn(previousName = "color") val color: String?
}
