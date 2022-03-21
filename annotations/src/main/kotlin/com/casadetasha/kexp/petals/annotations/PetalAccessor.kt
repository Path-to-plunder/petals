package com.casadetasha.kexp.petals.annotations

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.transactions.transaction

class OptionalNestedEntityManager<ACCESSOR: PetalAccessor<*, ENTITY, ID>, ENTITY: Entity<ID>, ID: Comparable<ID>>
    (private val loadedPetalId: ID?, private val loadClass: () -> ACCESSOR) {

    private var _nestedPetalId: ID? = loadedPetalId
    public var nestedPetalId: ID?
        @Synchronized get() = _nestedPetalId
        @Synchronized private set(value) { _nestedPetalId = value }

    private var _nestedPetalClass: ACCESSOR? = null
    public var nestedPetal: ACCESSOR?
        @Synchronized get() {
            _nestedPetalClass = _nestedPetalClass ?: loadClass()
            return _nestedPetalClass!!
        }
        @Synchronized set(value) {
            _nestedPetalId = value?.id
            _nestedPetalClass = value
        }

    fun hasUpdated(): Boolean {
        return loadedPetalId != nestedPetalId
    }
}

class NestedPetalManager<PETAL_ACCESSOR: PetalAccessor<*, ENTITY, ID>, ENTITY: Entity<ID>, ID: Comparable<ID>>
    (private val loadedPetalId: ID, private val loadEntityAccessor: () -> PETAL_ACCESSOR) {

    private var _nestedPetalId: ID = loadedPetalId
    public var nestedPetalId: ID
        @Synchronized get() = _nestedPetalId
        @Synchronized private set(value) { _nestedPetalId = value }

    private var _nestedPetalClass: PETAL_ACCESSOR? = null
    public var nestedPetal: PETAL_ACCESSOR
        @Synchronized get() {
            _nestedPetalClass = _nestedPetalClass ?: loadEntityAccessor()
            return _nestedPetalClass!!
        }
        @Synchronized set(value) {
            _nestedPetalId = value.id
            _nestedPetalClass = value
        }

    fun hasUpdated(): Boolean {
        return loadedPetalId != nestedPetalId
    }
}

abstract class PetalAccessor<ACCESSOR, out ENTITY: Entity<ID>, ID: Comparable<ID>> (val dbEntity: ENTITY, val id: ID) {

    /**
     * Update the object as is in the database. If any nested entities have changed, the associated ID will be updated.
     *
     * The update operation acts as a standalone transaction. If you want manual control over the transaction, call from
     * inside of a [transaction] with [performInsideStandaloneTransaction] set to false.
     *
     * Nested entities will not be updated by default. Call with "updateNestedDependencies = true" to store all nested
     * dependencies. This will only update first level nested dependencies, if you wish to update deeply nested
     * dependencies it must be done manually.
     */
    fun store(performInsideStandaloneTransaction: Boolean = true, updateNestedDependencies: Boolean = false): ACCESSOR {
        return when (performInsideStandaloneTransaction) {
            true -> transaction { storeInsideOfTransaction(updateNestedDependencies) }
            false -> storeInsideOfTransaction(updateNestedDependencies)
        }
    }

    protected abstract fun storeInsideOfTransaction(updateNestedDependencies: Boolean = false): ACCESSOR

    /** Delete the object from the database. */
    fun delete() = dbEntity.delete()
}

interface AccessorCompanion<ACCESSOR, in ENTITY: Entity<ID>, ID: Comparable<ID>> {

    fun load(id: ID): ACCESSOR?
    fun ENTITY.export(): ACCESSOR
}
