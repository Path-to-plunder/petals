package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.ReferencedBy

@Petal(tableName = "parent_petal", className = "ParentPetalClass", primaryKeyType = PetalPrimaryKey.UUID)
interface ParentPetal

@Petal(tableName = "nested_petal", className = "NestedPetalClass", primaryKeyType = PetalPrimaryKey.UUID)
interface NestedPetal

@PetalSchema(petal = ParentPetal::class)
interface ParentPetalSchema {
    val nestedPetal: NestedPetal
}

@PetalSchema(petal = NestedPetal::class)
interface NestedPetalSchema {
    val name: String
    @ReferencedBy("nestedPetal") val parents: ParentPetal
}
