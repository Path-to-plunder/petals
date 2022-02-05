package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.IntIdPetalEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.migration.`TableMigrations$int_id_petal`
import com.casadetasha.kexp.petals.processor.post.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class IntIdPetalSchemaTest: ContainerizedTestBase() {

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
    fun `Stores and loads petals`() {
        var petalEntityId: Int? = null
        transaction {
            val petalEntity = IntIdPetalEntity.new { column = "Columns are fun" }
            petalEntityId = petalEntity.id.value
        }

        transaction {
            val loadedPetal = IntIdPetalEntity[petalEntityId!!]
            assertThat(loadedPetal.column).isEqualTo("Columns are fun")
        }
    }

    @Test
    fun `Creates int IDs in order`() {
        transaction {
            val firstPetalEntity = IntIdPetalEntity.new { column = "Columns are fun" }
            val secondPetalEntity = IntIdPetalEntity.new { column = "Columns are still fun" }

            val firstPetalEntityId = firstPetalEntity.id.value
            val secondPetalEntityId = secondPetalEntity.id.value

            assertThat(secondPetalEntityId).isEqualTo(firstPetalEntityId+1)
        }
    }
}
