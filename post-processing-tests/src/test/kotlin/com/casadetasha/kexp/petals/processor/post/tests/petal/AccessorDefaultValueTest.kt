package com.casadetasha.kexp.petals.processor.post.tests.petal

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.PartiallyDefaultValuePetalEntity
import com.casadetasha.kexp.petals.accessor.PartiallyDefaultValuePetal
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$partially_default_value_petal`
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccessorDefaultValueTest: ContainerizedTestBase() {

    private val tableMigration: BasePetalMigration = `TableMigrations$partially_default_value_petal`()

    private val tableName: String by lazy {
        tableMigration.tableName
    }

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
    fun `store() without provided Int column value stores default value for column`() {
        val baseUuid = UUID.randomUUID()
        val id: UUID = PartiallyDefaultValuePetal.create(
            sporeCount = 2,
            color = "Blue",
            startingDefaultColor = "Also blue, but like a slightly darker shade",
            endingDefaultColor = "Not even close to blue. Like THE OPPOSITE.",
            secondColor = "Yellow",
            uuid = baseUuid
        ).store().id

        val loadedEntity = transaction {
            checkNotNull(PartiallyDefaultValuePetalEntity.findById(id)) { "Did not find petal $id in DB" }
        }

        assertThat(loadedEntity.id.value).isEqualTo(id)
        assertThat(loadedEntity.count).isEqualTo(10)
        assertThat(loadedEntity.sporeCount).isEqualTo(2)
        assertThat(loadedEntity.color).isEqualTo("Blue")
        assertThat(loadedEntity.startingDefaultColor).isEqualTo("Also blue, but like a slightly darker shade")
        assertThat(loadedEntity.endingDefaultColor).isEqualTo("Not even close to blue. Like THE OPPOSITE.")
        assertThat(loadedEntity.secondColor).isEqualTo("Yellow")
        assertThat(loadedEntity.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `store() without provided Long column value stores default value for column`() {
        val baseUuid = UUID.randomUUID()
        val id: UUID = PartiallyDefaultValuePetal.create(
            count = 1,
            color = "Blue",
            startingDefaultColor = "Also blue, but like a slightly darker shade",
            endingDefaultColor = "Not even close to blue. Like THE OPPOSITE.",
            secondColor = "Yellow",
            uuid = baseUuid
        ).store().id

        val loadedEntity = transaction {
            checkNotNull(PartiallyDefaultValuePetalEntity.findById(id)) { "Did not find petal $id in DB" }
        }

        assertThat(loadedEntity.id.value).isEqualTo(id)
        assertThat(loadedEntity.count).isEqualTo(1)
        assertThat(loadedEntity.sporeCount).isEqualTo(200)
        assertThat(loadedEntity.color).isEqualTo("Blue")
        assertThat(loadedEntity.startingDefaultColor).isEqualTo("Also blue, but like a slightly darker shade")
        assertThat(loadedEntity.endingDefaultColor).isEqualTo("Not even close to blue. Like THE OPPOSITE.")
        assertThat(loadedEntity.secondColor).isEqualTo("Yellow")
        assertThat(loadedEntity.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `store() without provided String column value stores default value for column`() {
        val baseUuid = UUID.randomUUID()
        val id: UUID = PartiallyDefaultValuePetal.create(
            count = 1,
            sporeCount = 2,
            startingDefaultColor = "Also blue, but like a slightly darker shade",
            secondColor = "Yellow",
            uuid = baseUuid
        ).store().id

        val loadedEntity = transaction {
            checkNotNull(PartiallyDefaultValuePetalEntity.findById(id)) { "Did not find petal $id in DB" }
        }

        assertThat(loadedEntity.id.value).isEqualTo(id)
        assertThat(loadedEntity.count).isEqualTo(1)
        assertThat(loadedEntity.sporeCount).isEqualTo(2)
        assertThat(loadedEntity.color).isEqualTo("default color")
        assertThat(loadedEntity.startingDefaultColor).isEqualTo("Also blue, but like a slightly darker shade")
        assertThat(loadedEntity.endingDefaultColor).isEqualTo("different default color")
        assertThat(loadedEntity.secondColor).isEqualTo("Yellow")
        assertThat(loadedEntity.uuid).isEqualTo(baseUuid)
    }
}
