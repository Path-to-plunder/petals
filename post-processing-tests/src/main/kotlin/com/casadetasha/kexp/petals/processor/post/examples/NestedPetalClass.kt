package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.AccessorCompanion
import com.casadetasha.kexp.petals.annotations.PetalAccessor
import org.jetbrains.exposed.sql.SizedIterable
import java.util.UUID
import kotlin.Boolean
import kotlin.String
import org.jetbrains.exposed.sql.transactions.transaction

public class NestedPetalClass(
    dbEntity: NestedPetalClassEntity,
    id: UUID,
    public var name: String
): PetalAccessor<NestedPetalClass, NestedPetalClassEntity, UUID>(dbEntity, id) {

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

        override fun loadAll(): SizedIterable<NestedPetalClass> {
            TODO("Not yet implemented")
        }

//        override val all: List<NestedPetalClass> = transaction { NestedPetalClass.all() }
    }

    override fun applyInsideTransaction(statement: NestedPetalClass.() -> Unit): NestedPetalClass {
        TODO("Not yet implemented")
    }
}
