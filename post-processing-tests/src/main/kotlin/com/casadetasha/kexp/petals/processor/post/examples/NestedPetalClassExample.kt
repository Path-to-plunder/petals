package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.PetalAccessor
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.SizedIterable
import java.util.UUID
import kotlin.Boolean
import kotlin.String
import org.jetbrains.exposed.sql.transactions.transaction

public class NestedPetalClassExample(
    dbEntity: ExampleNestedPetalClassEntity,
    id: UUID,
    public var name: String
): PetalAccessor<NestedPetalClassExample, ExampleNestedPetalClassEntity, UUID>(dbEntity, id) {

    public companion object: AccessorCompanion<NestedPetalClassExample, ExampleNestedPetalClassEntity, UUID>() {

        override fun load(id: UUID): NestedPetalClassExample? = transaction {
            ExampleNestedPetalClassEntity.findById(id)
        }?.export()

        override fun ExampleNestedPetalClassEntity.export(): NestedPetalClassExample =
            NestedPetalClassExample(
                dbEntity = this,
                name = name,
                id = id.value
            )

        override fun loadAll(): SizedIterable<NestedPetalClassExample> {
            TODO("Not yet implemented")
        }

        override fun storeInsideOfTransaction(nestedPetalClassExample: NestedPetalClassExample, updateNestedDependencies: Boolean): NestedPetalClassExample {
            val dbEntity = nestedPetalClassExample
            dbEntity.name = nestedPetalClassExample.name
            return nestedPetalClassExample
        }

    }

    override fun applyInsideTransaction(statement: NestedPetalClassExample.() -> Unit): NestedPetalClassExample {
        TODO("Not yet implemented")
    }

    override fun eagerLoadDependenciesInsideTransaction(): NestedPetalClassExample {
        TODO("Not yet implemented")
    }
}

abstract class AccessorCompanion<ACCESSOR: PetalAccessor<*, *, *>, in ENTITY: Entity<ID>, ID: Comparable<ID>> {

    abstract fun load(id: ID): ACCESSOR?
    abstract fun loadAll(): SizedIterable<ACCESSOR>
    abstract fun ENTITY.export(): ACCESSOR

    protected abstract fun storeInsideOfTransaction(accessor: ACCESSOR, updateNestedDependencies: Boolean = false): ACCESSOR

    /**
     * Update the object as is in the database. If any nested entities have changed, the associated ID will be updated.
     *
     * The update operation acts as a standalone transaction. If you want manual control over the transaction, call from
     * inside a [transaction] with [performInsideStandaloneTransaction] set to false.
     *
     * Nested entities will not be updated by default. Call with "updateNestedDependencies = true" to store all nested
     * dependencies. This will only update first level nested dependencies, if you wish to update deeply nested
     * dependencies it must be done manually.
     */
    fun store(accessor: ACCESSOR, performInsideStandaloneTransaction: Boolean = true, updateNestedDependencies: Boolean = false): ACCESSOR =
        runTransactionStatement(performInsideStandaloneTransaction) {
            storeInsideOfTransaction(accessor, updateNestedDependencies)
        }

    /** Delete the object from the database. */
    fun delete(accessor: ACCESSOR, performInsideStandaloneTransaction: Boolean = true) =
        PetalAccessor.runTransactionStatement(performInsideStandaloneTransaction) {
            accessor.dbEntity.delete()
        }

    private fun <RESULT> runTransactionStatement(performInsideStandaloneTransaction: Boolean, function: () -> RESULT): RESULT =
        when (performInsideStandaloneTransaction) {
            true -> transaction { function() }
            false -> function()
        }
}
