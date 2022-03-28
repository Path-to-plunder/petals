package com.casadetasha.kexp.petals.processor.post

import com.casadetasha.kexp.petals.annotations.Petal
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.ReferencedBy

@Petal(tableName = "optional_parent_petal", className = "OptionalParentPetalClass", primaryKeyType = PetalPrimaryKey.UUID)
interface OptionalParentPetal

@Petal(tableName = "optional_nested_petal", className = "OptionalNestedPetalClass", primaryKeyType = PetalPrimaryKey.UUID)
interface OptionalNestedPetal

@PetalSchema(petal = OptionalParentPetal::class)
interface OptionalParentPetalSchema {
    val name: String
    val nestedPetal: OptionalNestedPetal?
}

@PetalSchema(petal = OptionalNestedPetal::class)
interface OptionalNestedPetalSchema {
    val name: String
    @ReferencedBy("nestedPetal") val parents: OptionalParentPetal
}
