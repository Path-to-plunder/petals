package com.casadetasha.kexp.petals.annotations

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.atomic.AtomicBoolean

class OptionalNestedPetalManager<ACCESSOR: PetalAccessor<*, ENTITY, ID>, ENTITY: Entity<ID>, ID: Comparable<ID>>
    (private val loadedPetalId: ID?, private val loadEntityAccessor: () -> ACCESSOR?) {

    private val hasLoaded = AtomicBoolean(false)

    private var _nestedPetalId: ID? = loadedPetalId
    var nestedPetalId: ID?
        @Synchronized get() = _nestedPetalId
        @Synchronized private set(value) { _nestedPetalId = value }

    private var _nestedPetalClass: ACCESSOR? = null
    var nestedPetal: ACCESSOR?
        @Synchronized get() {
            if (hasLoaded.compareAndSet(false, true)) {
                _nestedPetalClass = _nestedPetalClass ?: transaction { loadEntityAccessor() }
                nestedPetalId = _nestedPetalClass?.id
            }
            return _nestedPetalClass
        }
        @Synchronized set(value) {
            _nestedPetalId = value?.id
            _nestedPetalClass = value
            hasLoaded.set(true)
        }

    val hasUpdated: Boolean get() { return loadedPetalId != nestedPetalId }

    fun eagerLoadAccessor() {
        _nestedPetalClass = loadEntityAccessor()
        hasLoaded.set(true)
    }
}

class NestedPetalManager<PETAL_ACCESSOR: PetalAccessor<*, ENTITY, ID>, ENTITY: Entity<ID>, ID: Comparable<ID>>
    (private val loadedPetalId: ID, private val loadEntityAccessor: () -> PETAL_ACCESSOR) {

    private var _nestedPetalId: ID = loadedPetalId
    var nestedPetalId: ID
        @Synchronized get() = _nestedPetalId
        @Synchronized private set(value) { _nestedPetalId = value }

    private var _nestedPetalClass: PETAL_ACCESSOR? = null
    var nestedPetal: PETAL_ACCESSOR
        @Synchronized get() {
            _nestedPetalClass = _nestedPetalClass ?: transaction { loadEntityAccessor() }
            nestedPetalId = _nestedPetalClass!!.id
            return _nestedPetalClass!!
        }
        @Synchronized set(value) {
            _nestedPetalId = value.id
            _nestedPetalClass = value
        }

    val hasUpdated: Boolean get() { return loadedPetalId != nestedPetalId }

    fun eagerLoadAccessor() {
        _nestedPetalClass = loadEntityAccessor()
    }
}

abstract class PetalAccessor<ACCESSOR, out ENTITY: Entity<ID>, ID: Comparable<ID>> (val dbEntity: ENTITY, val id: ID) {

    abstract fun applyInsideTransaction(statement: ACCESSOR.() -> Unit): ACCESSOR

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
    fun store(performInsideStandaloneTransaction: Boolean = true, updateNestedDependencies: Boolean = false): ACCESSOR =
        runTransactionStatement(performInsideStandaloneTransaction) {
            storeInsideOfTransaction(updateNestedDependencies)
        }

    /** Delete the object from the database. */
    fun delete(performInsideStandaloneTransaction: Boolean = true) =
        runTransactionStatement(performInsideStandaloneTransaction) {
            dbEntity.delete()
        }

    /** Prepare nested dependencies so they can be accessed outside a transaction. */
    fun eagerLoadDependencies(performInsideStandaloneTransaction: Boolean = true): ACCESSOR =
        runTransactionStatement(performInsideStandaloneTransaction) { eagerLoadDependenciesInsideTransaction() }

    protected abstract fun eagerLoadDependenciesInsideTransaction(): ACCESSOR

    protected abstract fun storeInsideOfTransaction(updateNestedDependencies: Boolean = false): ACCESSOR

    companion object {
        fun <RESULT> runTransactionStatement(performInsideStandaloneTransaction: Boolean, function: () -> RESULT): RESULT =
            when (performInsideStandaloneTransaction) {
                true -> transaction { function() }
                false -> function()
            }
    }
}

interface AccessorCompanion<ACCESSOR, in ENTITY: Entity<ID>, ID: Comparable<ID>> {

    fun load(id: ID): ACCESSOR?
    fun loadAll(): SizedIterable<ACCESSOR>
    fun ENTITY.export(): ACCESSOR

//    suspend fun flowAll(): Flow<ACCESSOR> = flow { loadAll().forEach { emit(it) } }
}
