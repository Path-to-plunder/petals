package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import java.util.UUID
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.exposed.sql.transactions.transaction

@Serializable
public data class BasicPetal constructor(
    public val id: Int?,
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

        transaction { findEntity().delete() }
    }

    fun store() {
        when (isStored) {
            false -> create()
            true -> update()
        }
    }

    private fun create(): BasicPetal {
        return transaction {
            when (val petalId = this@BasicPetal.id) {
                null -> BasicPetalEntity.new { this.setValues() }
                else -> BasicPetalEntity.new(petalId) { this.setValues() }
            }
        }.export()
    }

    private fun update() {
        val entity = findEntity()
        transaction { entity.setValues() }
    }

    private fun BasicPetalEntity.setValues() {
        renamed_color = this@BasicPetal.renamed_color
        renamed_count = this@BasicPetal.renamed_count
        renamed_secondColor = this@BasicPetal.renamed_secondColor
        renamed_sporeCount = this@BasicPetal.renamed_sporeCount
        renamed_uuid = this@BasicPetal.renamed_uuid
    }

    private fun findEntity(): BasicPetalEntity {
        checkNotNull(id) { "null ID found even though object was stored" }
        return checkNotNull(BasicPetalEntity.findById(id)) { "Could not update petal, no ID match found in DB." }
    }

    companion object {
        fun load(id: Int): BasicPetal? = transaction { BasicPetalEntity.findById(id) }
            ?.export()

        public fun BasicPetalEntity.export(): BasicPetal = BasicPetal(
            renamed_count = renamed_count,
            renamed_sporeCount = renamed_sporeCount,
            renamed_uuid = renamed_uuid,
            renamed_secondColor = renamed_secondColor,
            renamed_color = renamed_color,
            id = id.value
        ).apply { isStored = true }
    }
}
