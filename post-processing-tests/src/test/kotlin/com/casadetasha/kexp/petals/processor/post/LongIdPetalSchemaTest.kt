package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.LongIdPetalEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.migration.`TableMigrations$long_id_petal`
import com.casadetasha.kexp.petals.processor.post.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LongIdPetalSchemaTest: ContainerizedTestBase() {

    private val tableName: String by lazy { `TableMigrations$long_id_petal`().tableName }

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
        var petalEntityId: Long? = null
        transaction {
            val petalEntity = LongIdPetalEntity.new { column = "Some columns suck" }
            petalEntityId = petalEntity.id.value
        }

        transaction {
            val loadedPetal = LongIdPetalEntity[petalEntityId!!]
            assertThat(loadedPetal.column).isEqualTo("Some columns suck")
        }
    }

    @Test
    fun `Creates int IDs in order`() {
        transaction {
            val firstPetalEntity = LongIdPetalEntity.new { column = "This one doesn't sucks" }
            val secondPetalEntity = LongIdPetalEntity.new { column = "And this is the same column" }

            val firstPetalEntityId = firstPetalEntity.id.value
            val secondPetalEntityId = secondPetalEntity.id.value

            assertThat(secondPetalEntityId).isEqualTo(firstPetalEntityId+1)
        }
    }
}
