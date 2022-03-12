package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.post.examples.NestedPetalClass.Companion.export
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
public class ParentPetalClass private constructor(
    @Transient val entity: ParentPetalClassEntity? = null,
    @Serializable(with = UUIDSerializer::class) public val id: UUID? = null,
    @Serializable(with = UUIDSerializer::class) public var nestedPetalId: UUID? = null
) {

    @Transient
    public val nestedPetal: NestedPetalClass by lazy {
        petalIO!!.entity!!.nestedPetal
            .export()
            .apply {
                nestedPetalId = this.id
            }
    }

    public fun delete() = petalIO!!.delete(this)

    public fun store() = petalIO!!.store(this).export()

    public companion object {
        public fun load(id: UUID): ParentPetalClass? = ParentPetalClassIO.load(id)?.export()

        public fun ParentPetalClassEntity.export(): ParentPetalClass = transaction {
            val entity = this@export
            ParentPetalClass(
                petalIO = ParentPetalClassIO(entity),
                nestedPetalId = readValues[ParentPetalClassTable.nestedPetal].value,
                id = entity.id.value
            )
        }
    }
}

object ParentPetalClassIO() :
    PetalIO<ParentPetalClassEntity, ParentPetalClass> {

    private val isStored: Boolean
        @Synchronized
        get() = entity != null

    override fun delete(petal: ParentPetalClass): Unit {
        if (!isStored) {
            return
        }

        transaction { findBackingEntity()?.delete() }
    }

    override fun store(petal: ParentPetalClass): ParentPetalClassEntity = when (isStored) {
        false -> create(petal)
        true -> update(petal)
    }

    private fun create(accessor: ParentPetalClass): ParentPetalClassEntity = transaction {
        when (val petalId = accessor.id) {
            null -> ParentPetalClassEntity.new { this.storeValuesInBackend(accessor) }
            else -> ParentPetalClassEntity.new(petalId) { this.storeValuesInBackend(accessor) }
        }
    }

    private fun update(accessor: ParentPetalClass): ParentPetalClassEntity = transaction {
        val entity = checkNotNull(findBackingEntity()) {
            "Could not update petal, no ID match found in DB."
        }
        entity.storeValuesInBackend(accessor)
    }
//
//    private fun storeUpdate(): ParentPetalClassEntity {
//        ParentPetalClassTable.update({ ParentPetalClassTable.id eq parentPetalClass.id }) {
//            it[nestedPetal] = parentPetalClass.nestedPetalId
//        }
//        return ParentPetalClassEntity.findById(parentPetalClass.id!!)!!
//    }

    private fun ParentPetalClassEntity.storeValuesInBackend(accessor: ParentPetalClass): ParentPetalClassEntity {
        nestedPetal = accessor.nestedPetal.store().entity!!
        return this
    }

    private fun findBackingEntity(accessor: ParentPetalClass): ParentPetalClassEntity? {
        checkNotNull(accessor.id) { "Null petal ID found even though isStored is true" }
        return ParentPetalClassEntity.findById(accessor.id)
    }

    public companion object {

        public fun load(id: UUID): ParentPetalClassEntity? = transaction {
            ParentPetalClassEntity.findById(id)
        }
    }
}

interface PetalIO<S : Entity<*>, T> {

    fun delete(petal: T)

    fun store(petal: T): S
}
