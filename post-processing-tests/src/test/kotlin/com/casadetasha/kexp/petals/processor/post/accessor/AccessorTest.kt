package com.casadetasha.kexp.petals.processor.post.accessor

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.accessor.BasicPetal
import com.casadetasha.kexp.petals.accessor.BasicPetal.Companion.export
import com.casadetasha.kexp.petals.migration.`TableMigrations$int_id_petal`
import com.casadetasha.kexp.petals.processor.post.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccessorTest: ContainerizedTestBase() {

    private val tableName: String by lazy { `TableMigrations$int_id_petal`().tableName }

    @BeforeTest
    fun setup() {
        PetalTables.setupAndMigrateTables(datasource)
    }

    @AfterTest
    fun teardown() {
        datasource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM \"$tableName\"").execute()
        }
    }

    @Test
    fun `Exports to accessor`() {
        val baseUuid = UUID.randomUUID()
        val petalEntity: BasicPetal = transaction {
            BasicPetalEntity.new {
                renamed_count = 1
                renamed_sporeCount = 2
                renamed_color = "Blue"
                renamed_secondColor = "Yellow"
                renamed_uuid = baseUuid
            }.export()
        }

        assertThat(petalEntity.renamed_count).isEqualTo(1)
    }

    @Test
    fun `load() loads stored data`() {
        val baseUuid = UUID.randomUUID()
        val petalId: Int = transaction {
            BasicPetalEntity.new {
                renamed_count = 1
                renamed_sporeCount = 2
                renamed_color = "Blue"
                renamed_secondColor = "Yellow"
                renamed_uuid = baseUuid
            }.id.value
        }

        val loadedPetal = checkNotNull(BasicPetal.load(petalId))
        assertThat(loadedPetal.id).isEqualTo(petalId)
        assertThat(loadedPetal.renamed_count).isEqualTo(1)
        assertThat(loadedPetal.renamed_sporeCount).isEqualTo(2)
        assertThat(loadedPetal.renamed_color).isEqualTo("Blue")
        assertThat(loadedPetal.renamed_secondColor).isEqualTo("Yellow")
        assertThat(loadedPetal.renamed_uuid).isEqualTo(baseUuid)
    }
}
