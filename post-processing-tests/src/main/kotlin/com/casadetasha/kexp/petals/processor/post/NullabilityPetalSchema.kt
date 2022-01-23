package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal

@Petal(tableName = "starting_nullable_petal", className = "StartingNullablePetal", version = 1)
interface StartingNullablePetalSchemaV1 {
    val color: String?
}

@Petal(tableName = "starting_nullable_petal", className = "StartingNullablePetal", version = 2)
interface StartingNullablePetalSchemaV2 {
    @AlterColumn val color: String
}

@Petal(tableName = "starting_nullable_petal", className = "StartingNullablePetal", version = 3)
interface StartingNullablePetalSchemaV3 {
    val color: String
    val secondColor: String?
}

@Petal(tableName = "starting_non_nullable_petal", className = "StartingNonNullablePetal", version = 1)
interface StartingNonNullablePetalSchemaV1 {
    val color: String
}

@Petal(tableName = "starting_non_nullable_petal", className = "StartingNonNullablePetal", version = 2)
interface StartingNonNullablePetalSchemaV2 {
    @AlterColumn val color: String?
}

@Petal(tableName = "starting_non_nullable_petal", className = "StartingNonNullablePetal", version = 3)
interface StartingNonNullablePetalSchemaV3 {
    val color: String?
    val secondColor: String
}
