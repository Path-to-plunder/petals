package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal

import kotlin.Int

@Petal(tableName = "basic_petal", version = 1)
interface BasicPetalSchemaV1 {
    val name: String
    val count: Int
}

@Petal(tableName = "basic_petal", version = 2)
interface BasicPetalSchemaV2 {
    val name: String
    val count: Int

    val secondName: String
    val secondCount: Int
}
