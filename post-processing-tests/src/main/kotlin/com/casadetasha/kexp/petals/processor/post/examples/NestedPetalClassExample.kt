package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.AccessorCompanion
import com.casadetasha.kexp.petals.annotations.PetalAccessor
import org.jetbrains.exposed.sql.SizedIterable
import java.util.UUID
import kotlin.Boolean
import kotlin.String
import org.jetbrains.exposed.sql.transactions.transaction

public class NestedPetalClassExample(
    dbEntity: NestedPetalClassEntity,
    id: UUID,
    public var name: String
): PetalAccessor<NestedPetalClassExample, NestedPetalClassEntity, UUID>(dbEntity, id) {

    override fun storeInsideOfTransaction(updateNestedDependencies: Boolean): NestedPetalClassExample {
        dbEntity.name = this@NestedPetalClassExample.name
        return this
    }

    public companion object: AccessorCompanion<NestedPetalClassExample, NestedPetalClassEntity, UUID> {

        override fun load(id: UUID): NestedPetalClassExample? = transaction {
            NestedPetalClassEntity.findById(id)
        }?.export()

        override fun NestedPetalClassEntity.export(): NestedPetalClassExample =
            NestedPetalClassExample(
                dbEntity = this,
                name = name,
                id = id.value
            )

        override fun loadAll(): SizedIterable<NestedPetalClassExample> {
            TODO("Not yet implemented")
        }

//        override val all: List<NestedPetalClass> = transaction { NestedPetalClass.all() }
    }

    override fun applyInsideTransaction(statement: NestedPetalClassExample.() -> Unit): NestedPetalClassExample {
        TODO("Not yet implemented")
    }

    override fun eagerLoadDependenciesInsideTransaction(): NestedPetalClassExample {
        TODO("Not yet implemented")
    }
}
