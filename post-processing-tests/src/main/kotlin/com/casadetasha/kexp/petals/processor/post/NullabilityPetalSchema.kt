package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal

@Petal(tableName = "create_as_nullable_petal", version = 1)
interface CreateAsNullablePetalSchema {
    val color: String?
}

@Petal(tableName = "nullability_petal", version = 1)
interface NullabilityPetalSchemaV1 {
    val color: String
}
//
//@Petal(tableName = "nullability_petal", version = 2)
//interface NullabilityPetalSchemaV2 {
//    @AlterColumn(previousName = "color") val color: String?
//}
