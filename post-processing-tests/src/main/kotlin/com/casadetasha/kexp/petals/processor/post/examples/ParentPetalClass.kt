package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.post.examples.NestedPetalClass.Companion.export
import kotlinx.serialization.SerialName
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
public class ParentPetalClass private constructor(
    @Transient val petalEntity: ParentPetalClassEntity? = null,
    @Serializable(with = UUIDSerializer::class)
    public val id: UUID? = null,
    @Serializable(with = UUIDSerializer::class)
    @SerialName("nestedPetalId")
    private var _nestedPetalId: UUID? = null
) {

    public var nestedPetalId: UUID?
        get() = _nestedPetalId
        private set(value) { _nestedPetalId = value }

    private val entity: ParentPetalClassEntity by lazy {
        checkNotNull(petalEntity) { "Cannot perform DB operations on a parsed ParentPetalClass." }
    }

    private var nestedPetal: NestedPetalClass
        get() = entity.nestedPetal.export()
        @Synchronized set(value) {
            _nestedPetalId = value.id
            entity.nestedPetal = value.petalEntity!!
        }

    fun store(shouldStoreDependencyChain: Boolean): ParentPetalClass = transaction {
        if (shouldStoreDependencyChain) { this@ParentPetalClass.nestedPetal.store() }

        return@transaction entity.apply {
            nestedPetal = this@ParentPetalClass.nestedPetal.petalEntity!!
        }
    }.export()

    fun delete(): Unit {
        transaction { entity.delete() }
    }

    public companion object {

        public fun create(
            id: UUID? = null,
            nestedPetal: NestedPetalClass
        ): ParentPetalClass = transaction {
            val storeValues: ParentPetalClassEntity.() -> Unit = {
                this.nestedPetal = nestedPetal.petalEntity!!
            }

            return@transaction when (id) {
                null -> ParentPetalClassEntity.new { storeValues() }
                else -> ParentPetalClassEntity.new(id) { storeValues() }
            }
        }.export()

        public fun load(id: UUID): ParentPetalClass? = transaction {
            ParentPetalClassEntity.findById(id)
        }?.export()

        public fun ParentPetalClassEntity.export(): ParentPetalClass = transaction {
            val entity = this@export
            return@transaction ParentPetalClass(
                petalEntity = entity,
                nestedPetalId = readValues[ParentPetalClassTable.nestedPetal].value,
                id = entity.id.value
            )
        }
    }
}