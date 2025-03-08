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
    val name: String
    val nestedPetal: NestedPetal
}

@PetalSchema(petal = NestedPetal::class)
interface NestedPetalSchema {
    val name: String
    @ReferencedBy("nestedPetal") val parents: ParentPetal
}

@Petal(tableName = "int_id_parent_petal", className = "IntIdParentPetalClass", primaryKeyType = PetalPrimaryKey.INT)
interface IntIdParentPetal

@Petal(tableName = "int_id_nested_petal", className = "IntIdNestedPetalClass", primaryKeyType = PetalPrimaryKey.INT)
interface IntIdNestedPetal

@PetalSchema(petal = IntIdParentPetal::class)
interface IntIdParentPetalSchema {
    val name: String
    val nestedPetal: IntIdNestedPetal
}

@PetalSchema(petal = IntIdNestedPetal::class)
interface IntIdNestedPetalSchema {
    val name: String
    @ReferencedBy("nestedPetal") val parents: IntIdParentPetal
}

@Petal(tableName = "long_id_parent_petal", className = "LongIdParentPetalClass", primaryKeyType = PetalPrimaryKey.LONG)
interface LongIdParentPetal

@Petal(tableName = "long_id_nested_petal", className = "LongIdNestedPetalClass", primaryKeyType = PetalPrimaryKey.LONG)
interface LongIdNestedPetal

@PetalSchema(petal = LongIdParentPetal::class)
interface LongIdParentPetalSchema {
    val name: String
    val nestedPetal: LongIdNestedPetal
}

@PetalSchema(petal = LongIdNestedPetal::class)
interface LongIdNestedPetalSchema {
    val name: String
    @ReferencedBy("nestedPetal") val parents: LongIdParentPetal
}
