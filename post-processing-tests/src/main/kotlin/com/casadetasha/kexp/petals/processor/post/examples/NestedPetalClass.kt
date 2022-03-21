package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.AccessorCompanion
import com.casadetasha.kexp.petals.annotations.EntityAccessor
import java.util.UUID
import kotlin.Boolean
import kotlin.String
import kotlin.Unit
import org.jetbrains.exposed.sql.transactions.transaction

public class NestedPetalClass(
    dbEntity: NestedPetalClassEntity,
    id: UUID,
    public var name: String
): EntityAccessor<NestedPetalClass, NestedPetalClassEntity, UUID>(dbEntity, id) {

    override fun storeInsideOfTransaction(updateNestedDependencies: Boolean): NestedPetalClass {
        dbEntity.name = this@NestedPetalClass.name
        return this
    }

    public companion object: AccessorCompanion<NestedPetalClass, NestedPetalClassEntity, UUID> {
        override fun load(id: UUID): NestedPetalClass? = transaction {
            NestedPetalClassEntity.findById(id)
        }?.export()

        override fun NestedPetalClassEntity.export(): NestedPetalClass =
            NestedPetalClass(
                dbEntity = this,
                name = name,
                id = id.value
            )
    }
}
