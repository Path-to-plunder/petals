package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.MigratedPetalEntity
import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import java.util.UUID
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
public data class MigratedPetal constructor(
    private val id: Int?,
    public var renamed_color: String,
    public var renamed_count: Int,
    public var renamed_secondColor: String,
    public var renamed_sporeCount: Long,
    @Serializable(with = UUIDSerializer::class)
    public var renamed_uuid: UUID
) {

    @Transient
    public var isStored: Boolean = false
        @Synchronized get
        @Synchronized private set

    public fun destroy() {
        if (!isStored) return

        transaction { findBackingEntity()?.delete() }
    }

    fun store(): MigratedPetal {
        val entity = when (isStored) {
            false -> create()
            true -> update()
        }
        return entity
    }

    private fun create(): MigratedPetal {
        return transaction {
            when (val petalId = this@MigratedPetal.id) {
                null -> MigratedPetalEntity.new { this.setValues() }
                else -> MigratedPetalEntity.new(petalId) { this.setValues() }
            }
        }.export()
    }

    private fun update(): MigratedPetal {
        return transaction {
            val entity = checkNotNull(findBackingEntity()) { "Could not update petal, no ID match found in DB." }
            entity.setValues()
            return@transaction entity
        }.export()
    }

    private fun MigratedPetalEntity.setValues() {
        renamed_color = this@MigratedPetal.renamed_color
        renamed_count = this@MigratedPetal.renamed_count
        renamed_secondColor = this@MigratedPetal.renamed_secondColor
        renamed_sporeCount = this@MigratedPetal.renamed_sporeCount
        renamed_uuid = this@MigratedPetal.renamed_uuid
    }

    private fun findBackingEntity(): MigratedPetalEntity? {
        checkNotNull(id) { "null ID found even though object was stored" }
        return MigratedPetalEntity.findById(id)
    }

    companion object {
        fun load(id: Int): MigratedPetal? = transaction { MigratedPetalEntity.findById(id) }
            ?.export()

        public fun MigratedPetalEntity.export(): MigratedPetal = MigratedPetal(
            renamed_count = renamed_count,
            renamed_sporeCount = renamed_sporeCount,
            renamed_uuid = renamed_uuid,
            renamed_secondColor = renamed_secondColor,
            renamed_color = renamed_color,
            id = id.value
        ).apply { isStored = true }
    }
}
