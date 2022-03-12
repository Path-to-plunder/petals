package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import java.util.UUID
import kotlin.Boolean
import kotlin.String
import kotlin.Unit
import kotlin.jvm.Synchronized
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
public data class NestedPetalClass(
    @Transient val petalEntity: NestedPetalClassEntity? = null,
    @Serializable(with = UUIDSerializer::class)
    public val id: UUID? = null,
    public var name: String
) {

    private val entity: NestedPetalClassEntity by lazy {
        checkNotNull(petalEntity) { "Cannot perform DB operations on a parsed ParentPetalClass." }
    }

    public fun delete(): Unit {
        transaction { entity.delete() }
    }

    public fun store(): NestedPetalClass = transaction {
            entity.storeValuesInBackend()
        }.export()

    private fun create(): NestedPetalClass = transaction {
        when (val petalId = this@NestedPetalClass.id)  {
            null -> NestedPetalClassEntity.new { this.storeValuesInBackend() }
            else -> NestedPetalClassEntity.new(petalId) { this.storeValuesInBackend() }
        }
    }.export()

    private fun update(): NestedPetalClass = transaction {
        val entity = checkNotNull(findBackingEntity()) {
            "Could not update petal, no ID match found in DB."
        }
        entity.storeValuesInBackend()
    }.export()

    private fun NestedPetalClassEntity.storeValuesInBackend(): NestedPetalClassEntity {
        name = this@NestedPetalClass.name
        return this
    }

    private fun findBackingEntity(): NestedPetalClassEntity? {
        checkNotNull(id) { "Null petal ID found even though isStored is true" }
        return NestedPetalClassEntity.findById(id)
    }

    public companion object {
        public fun load(id: UUID): NestedPetalClass? = transaction {
            NestedPetalClassEntity.findById(id)
        }?.export()

        public fun NestedPetalClassEntity.export(): NestedPetalClass =
            NestedPetalClass(
                name = name,
                id = id.value
            )
    }
}
