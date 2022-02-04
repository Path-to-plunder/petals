package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.*
import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.ClassRule
import org.junit.rules.ExternalResource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class BasicPetalSchemaTest {

    companion object {

        private lateinit var datasource: HikariDataSource

        var dbContainer: JdbcDatabaseContainer<*> = PostgreSQLContainer("postgres")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password")

        @ClassRule
        @JvmField
        val resource: ExternalResource = object : ExternalResource() {

            override fun before() {
                dbContainer.start()
                runMigrations()
            }

            private fun runMigrations() {
                datasource = HikariDataSource(
                    HikariConfig().apply {
                        jdbcUrl = dbContainer.jdbcUrl;
                        username = dbContainer.username;
                        password = dbContainer.password;
                        driverClassName = dbContainer.driverClassName;
                    })

                Database.connect(datasource)
            }

            override fun after() {
                dbContainer.close()
            }
        }
    }

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
