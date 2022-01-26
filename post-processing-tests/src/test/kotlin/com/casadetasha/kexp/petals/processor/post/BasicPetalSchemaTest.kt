package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.BasicPetalTable
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.annotations.MetaTableInfo
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Rule
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test

private val json = Json { prettyPrint = true }

class BasicPetalSchemaTest {

    lateinit var decodedPetalMigration: PetalMigration

    @get:Rule
    var genericContainer: PostgreSQLContainer<*> = PostgreSQLContainer("postgres")
        .withDatabaseName("test_db")
        .withUsername("test_user")
        .withPassword("test_password")

    private lateinit var datasource: HikariDataSource

    @BeforeTest
    fun setup() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$basic_petal`().petalJson)

        val jdbcContainer =  genericContainer as JdbcDatabaseContainer<*>;

        datasource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = jdbcContainer.jdbcUrl;
                username = jdbcContainer.username;
                password = jdbcContainer.password;
                driverClassName = jdbcContainer.driverClassName;
            })

        Database.connect(datasource)
        PetalTables.setupAndMigrateTables(datasource)
    }

    @Test
    fun `Creates table creation migration with all supported types`() {
        assertThat(decodedPetalMigration.schemaMigrations[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE \"basic_petal\" (" +
              " id SERIAL PRIMARY KEY," +
              " \"checkingVarChar\" CHARACTER VARYING(10) NOT NULL," +
              " \"checkingString\" TEXT NOT NULL," +
              " \"checkingInt\" INT NOT NULL," +
              " \"checkingUUID\" uuid NOT NULL," +
              " \"checkingLong\" BIGINT NOT NULL" +
              " )"
        )
    }

    @Test
    fun `Creates alter table migration with dropping and adding all supported types`() {
        assertThat(decodedPetalMigration.schemaMigrations[2]!!.migrationSql)
            .isEqualTo("ALTER TABLE \"basic_petal\"" +
              " DROP COLUMN \"checkingVarChar\"," +
              " DROP COLUMN \"checkingString\"," +
              " DROP COLUMN \"checkingInt\"," +
              " DROP COLUMN \"checkingUUID\"," +
              " DROP COLUMN \"checkingLong\"," +
              " ADD COLUMN \"color\" TEXT NOT NULL," +
              " ADD COLUMN \"count\" INT NOT NULL," +
              " ADD COLUMN \"secondColor\" CHARACTER VARYING(10) NOT NULL," +
              " ADD COLUMN \"uuid\" uuid NOT NULL," +
              " ADD COLUMN \"sporeCount\" BIGINT NOT NULL")
    }

    @Test
    fun `Creates alter table migration with renaming of all supported types`() {
        assertThat(decodedPetalMigration.schemaMigrations[3]!!.migrationSql)
            .isEqualTo("ALTER TABLE \"basic_petal\"" +
              " RENAME COLUMN \"count\" TO \"renamed_count\"," +
              " RENAME COLUMN \"sporeCount\" TO \"renamed_sporeCount\"," +
              " RENAME COLUMN \"uuid\" TO \"renamed_uuid\"," +
              " RENAME COLUMN \"secondColor\" TO \"renamed_secondColor\"," +
              " RENAME COLUMN \"color\" TO \"renamed_color\"")
    }

//    @Test
//    fun `Loads stored petals`() {
////        val testMigrationTable = `TableMigrations$basic_petal`()
////        val testMigrationTableInfo = MetaTableInfo.loadTableInfo(datasource, testMigrationTable.tableName)
//
//        val baseUuid = UUID.randomUUID();
//        var petalEntityId: Int? = null
//        transaction {
//            val petalEntity = BasicPetalEntity.new {
//                count = 1
//                sporeCount = 2
//                color = "Blue"
//                secondColor = "Yellow"
//                uuid = baseUuid
//            }
//
//            petalEntityId = petalEntity.id.value
//        }
//
//        transaction {
//            val loadedPetal = BasicPetalEntity[petalEntityId!!]
//            assertThat(loadedPetal.count).isEqualTo(1)
//            assertThat(loadedPetal.sporeCount).isEqualTo(2)
//            assertThat(loadedPetal.color).isEqualTo("Blue")
//            assertThat(loadedPetal.secondColor).isEqualTo("Yellow")
//            assertThat(loadedPetal.uuid).isEqualTo(baseUuid)
//        }
//    }
}
