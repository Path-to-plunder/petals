package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.VarChar
import java.util.*

@Petal(tableName = "basic_petal", className = "BasicPetal", primaryKeyType = PetalPrimaryKey.UUID)
interface BasicPetal

@PetalSchema(petal = BasicPetal::class)
interface BasicPetalSchemaV1 {
    val uuid: UUID
    var color: String?
    @VarChar(charLimit = 10) var secondColor: String
    var count: Int
    var sporeCount: Long
}

@Petal(tableName = "user", className = "User", primaryKeyType = PetalPrimaryKey.UUID)
interface UserPetal

@PetalSchema(petal = UserPetal::class)
interface UserPetalV1 {
    @VarChar(charLimit = 50) var name: String
}
