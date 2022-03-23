package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.AccessorCompanion
import com.casadetasha.kexp.petals.annotations.PetalAccessor
import com.casadetasha.kexp.petals.annotations.NestedPetalManager
import com.casadetasha.kexp.petals.processor.post.examples.NestedPetalClass.Companion.export
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
public class ParentPetalClass private constructor(
    dbEntity: ParentPetalClassEntity,
    id: UUID,
    nestedPetalId: UUID
): PetalAccessor<ParentPetalClass, ParentPetalClassEntity, UUID>(dbEntity, id) {

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
    public var nestedPetal: NestedPetalClass by nestedPetalManager::nestedPetal

    override fun storeInsideOfTransaction(updateNestedDependencies: Boolean): ParentPetalClass {
        if (updateNestedDependencies) { storeFullDependencyChain() }

        return dbEntity.apply {
            if (nestedPetalManager.hasUpdated()) { nestedPetal = this@ParentPetalClass.nestedPetal.dbEntity }
        }.export()
    }

    private fun storeFullDependencyChain() {
        nestedPetal.store(performInsideStandaloneTransaction = false)
    }

    public companion object: AccessorCompanion<ParentPetalClass, ParentPetalClassEntity, UUID> {

//        override val all: Flow<ParentPetalClass>
//            get() = flow {
//                ParentPetalClassEntity.all().forEach { emit(it.export()) }
//            }

        public fun create(
            id: UUID? = null,
            nestedPetal: NestedPetalClass
        ): ParentPetalClass = transaction {
            val storeValues: ParentPetalClassEntity.() -> Unit = {
                this.nestedPetal = nestedPetal.dbEntity
            }

            return@transaction when (id) {
                null -> ParentPetalClassEntity.new { storeValues() }
                else -> ParentPetalClassEntity.new(id) { storeValues() }
            }
        }.export()

        override fun load(id: UUID): ParentPetalClass? = transaction {
            ParentPetalClassEntity.findById(id)
        }?.export()

        override fun ParentPetalClassEntity.export(): ParentPetalClass = transaction {
            val entity = this@export
            return@transaction ParentPetalClass(
                dbEntity = entity,
                id = entity.id.value,
                nestedPetalId = readValues[ParentPetalClassTable.nestedPetal].value
            )
        }
    }

    override fun applyInsideTransaction(statement: ParentPetalClass.() -> Unit): ParentPetalClass {
        TODO("Not yet implemented")
    }
}
