package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import com.casadetasha.kexp.petals.processor.post.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class MigratedPetalSchemaTest: ContainerizedTestBase() {

    private val tableName: String by lazy { `TableMigrations$basic_petal`().tableName }

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
    fun `Loads stored petals`() {
        val baseUuid = UUID.randomUUID();
        var petalEntityId: Int? = null
        transaction {
            val petalEntity = BasicPetalEntity.new {
                renamed_count = 1
                renamed_sporeCount = 2
                renamed_color = "Blue"
                renamed_secondColor = "Yellow"
                renamed_uuid = baseUuid
            }

            petalEntityId = petalEntity.id.value
        }

        transaction {
            val loadedPetal = BasicPetalEntity[petalEntityId!!]
            assertThat(loadedPetal.renamed_count).isEqualTo(1)
            assertThat(loadedPetal.renamed_sporeCount).isEqualTo(2)
            assertThat(loadedPetal.renamed_color).isEqualTo("Blue")
            assertThat(loadedPetal.renamed_secondColor).isEqualTo("Yellow")
            assertThat(loadedPetal.renamed_uuid).isEqualTo(baseUuid)
        }
    }

    @Test
    fun `Creates int IDs in order`() {
        transaction {
            val firstPetalEntity = BasicPetalEntity.new {
                renamed_count = 1
                renamed_sporeCount = 2
                renamed_color = "Blue"
                renamed_secondColor = "Yellow"
                renamed_uuid = UUID.randomUUID()
            }

            val secondPetalEntity = BasicPetalEntity.new {
                renamed_count = 1
                renamed_sporeCount = 2
                renamed_color = "Blue"
                renamed_secondColor = "Yellow"
                renamed_uuid = UUID.randomUUID()
            }

            val firstPetalEntityId = firstPetalEntity.id.value
            val secondPetalEntityId = secondPetalEntity.id.value

            assertThat(secondPetalEntityId).isEqualTo(firstPetalEntityId+1)
        }
    }
}
