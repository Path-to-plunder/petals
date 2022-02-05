package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.UuidIdPetalEntity
import com.casadetasha.kexp.petals.migration.`TableMigrations$uuid_id_petal`
import com.casadetasha.kexp.petals.processor.post.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class UuidPetalSchemaTest: ContainerizedTestBase() {

    private val tableName: String by lazy { `TableMigrations$uuid_id_petal`().tableName }

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
        var petalEntityId: UUID? = null
        transaction {
            val petalEntity = UuidIdPetalEntity.new { column = "Dis column, is bananas. B, A N A N A S" }
            petalEntityId = petalEntity.id.value
        }

        transaction {
            val loadedPetal = UuidIdPetalEntity[petalEntityId!!]
            assertThat(loadedPetal.column).isEqualTo("Dis column, is bananas. B, A N A N A S")
        }
    }

    @Test
    fun `Creates unique UUIDs`() {
        transaction {
            val firstPetalEntity = UuidIdPetalEntity.new { column = "Dis column value? Not bananas." }
            val secondPetalEntity = UuidIdPetalEntity.new { column = "Dis column value? Also not bananas." }

            val firstPetalEntityId: UUID = firstPetalEntity.id.value
            val secondPetalEntityId: UUID = secondPetalEntity.id.value

            assertThat(secondPetalEntityId).isNotEqualTo(firstPetalEntityId)
        }
    }
}
