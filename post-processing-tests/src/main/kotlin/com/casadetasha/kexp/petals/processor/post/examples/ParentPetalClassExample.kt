package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.AccessorCompanion
import com.casadetasha.kexp.petals.annotations.PetalAccessor
import com.casadetasha.kexp.petals.annotations.NestedPetalManager
import com.casadetasha.kexp.petals.processor.post.examples.NestedPetalClassExample.Companion.export
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

/**
 * ### Accessor class for [ParentPetalClassEntity].
 *
 * * Allows modifying values inline that will not be stored to the db until [store] or [storeInsideOfTransaction] is
 * manually called.
 *
 * * Provides CRUD operation methods inside self-contained [transaction]s.
 */
public class ParentPetalClassExample private constructor(
    dbEntity: ParentPetalClassEntity,
    id: UUID,
    nestedPetalId: UUID
): PetalAccessor<ParentPetalClassExample, ParentPetalClassEntity, UUID>(dbEntity, id) {

    private val nestedPetalManager by lazy {
        NestedPetalManager(nestedPetalId) { dbEntity.nestedPetal.export() }
    }

    /**
     * The ID for an associated NestedPetal. Accessing this will never trigger a DB transaction.
     */
    public val nestedPetalId: UUID by nestedPetalManager::nestedPetalId

    /**
     * An associated NestedPetalClass. Reading this value will trigger a DB transaction the first time, unless the
     * instance of this object was manually exported from a ParentPetalClassEntity that eager loaded this value.
     */
    public var nestedPetal: NestedPetalClassExample by nestedPetalManager::nestedPetal

    override fun storeInsideOfTransaction(updateNestedDependencies: Boolean): ParentPetalClassExample {
        if (updateNestedDependencies) { storeFullDependencyChain() }

        return dbEntity.apply {
            if (nestedPetalManager.hasUpdated) { nestedPetal = this@ParentPetalClassExample.nestedPetal.dbEntity }
        }.export()
    }

    private fun storeFullDependencyChain() {
        nestedPetal.store(performInsideStandaloneTransaction = false)
    }

    public companion object: AccessorCompanion<ParentPetalClassExample, ParentPetalClassEntity, UUID> {

//        override val all: Flow<ParentPetalClass>
//            get() = flow {
//                ParentPetalClassEntity.all().forEach { emit(it.export()) }
//            }

        public fun create(
            id: UUID? = null,
            nestedPetal: NestedPetalClassExample
        ): ParentPetalClassExample = transaction {
            val storeValues: ParentPetalClassEntity.() -> Unit = {
                this.nestedPetal = nestedPetal.dbEntity
            }

            return@transaction when (id) {
                null -> ParentPetalClassEntity.new { storeValues() }
                else -> ParentPetalClassEntity.new(id) { storeValues() }
            }
        }.export()

        override fun load(id: UUID): ParentPetalClassExample? = transaction {
            ParentPetalClassEntity.findById(id)
        }?.export()

        fun ParentPetalClassEntity.eagerLoadDependencies(): ParentPetalClassEntity =
            load(ParentPetalClassEntity::nestedPetal)

        override fun ParentPetalClassEntity.export(): ParentPetalClassExample = transaction {
            val entity = this@export
            return@transaction ParentPetalClassExample(
                dbEntity = entity,
                id = entity.id.value,
                nestedPetalId = readValues[ParentPetalClassTable.nestedPetal].value
            )
        }

        override fun loadAll(): SizedIterable<ParentPetalClassExample> {
            TODO("Not yet implemented")
        }
    }

    override fun applyInsideTransaction(statement: ParentPetalClassExample.() -> Unit): ParentPetalClassExample {
        TODO("Not yet implemented")
    }

    override fun eagerLoadDependenciesInsideTransaction(): ParentPetalClassExample {
        TODO("Not yet implemented")
    }
}
