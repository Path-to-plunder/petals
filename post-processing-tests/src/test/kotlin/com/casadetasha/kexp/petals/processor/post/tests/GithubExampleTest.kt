package com.casadetasha.kexp.petals.processor.post.tests

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.DataPetalEntity
import com.casadetasha.kexp.petals.accessor.DataPetal
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.data.DataPetalData
import com.casadetasha.kexp.petals.data.asData
import com.casadetasha.kexp.petals.migration.`TableMigrations$data_petal`
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class GithubExampleTest: ContainerizedTestBase() {

    private val tableMigration: BasePetalMigration = `TableMigrations$data_petal`()
    private val tableName: String by lazy { tableMigration.tableName }

    @BeforeTest
    fun setup() {
        tableMigration.migrateToLatest(datasource)
    }

    @AfterTest
    fun teardown() {
        datasource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM \"$tableName\"").execute()
        }
    }

    @Test
    fun `accessor exports to data`() {
        val baseUuid = UUID.randomUUID()
        val petalData: DataPetalData = DataPetal.create(
                count = 1,
                sporeCount = 2,
                color = "Blue",
                secondColor = "Yellow",
                uuid = baseUuid
        ).asData()

        assertThat(petalData.count).isEqualTo(1)
        assertThat(petalData.sporeCount).isEqualTo(2)
        assertThat(petalData.color).isEqualTo("Blue")
        assertThat(petalData.secondColor).isEqualTo("Yellow")
        assertThat(petalData.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `entity exports to data`() {
        val baseUuid = UUID.randomUUID()
        val petalData: DataPetalData = transaction {
            DataPetalEntity.new {
                count = 3
                sporeCount = 4
                color = "Black"
                secondColor = "Red"
                uuid = baseUuid
            }.asData()
        }

        assertThat(petalData.count).isEqualTo(3)
        assertThat(petalData.sporeCount).isEqualTo(4)
        assertThat(petalData.color).isEqualTo("Black")
        assertThat(petalData.secondColor).isEqualTo("Red")
        assertThat(petalData.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `data petal parses to json`() {
        val expectedJson = Json.decodeFromString<JsonElement>(
            """
               {
                   "id":"737856fa-c99a-446d-b4bc-5c3383e6e00d",
                   "count":3,
                   "sporeCount":4,
                   "color":"Black",
                   "secondColor":"Red",
                   "uuid":"737856fa-c99a-446d-b4bc-5c3383e6e00d"
               }
           """.trimIndent()
        ).jsonObject


        val jsonValue = Json.encodeToJsonElement(
            DataPetal.create(
                id = UUID.fromString("737856fa-c99a-446d-b4bc-5c3383e6e00d"),
                count = 3,
                sporeCount = 4,
                color = "Black",
                secondColor = "Red",
                uuid = UUID.fromString("737856fa-c99a-446d-b4bc-5c3383e6e00d")
            ).asData()
        ).jsonObject

        assertThat(jsonValue).isEqualTo(expectedJson)
    }
}
