package com.casadetasha.kexp.petals.processor.post.accessor

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.DefaultValuePetalEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.accessor.BasicPetal
import com.casadetasha.kexp.petals.accessor.BasicPetal.Companion.export
import com.casadetasha.kexp.petals.accessor.DefaultValuePetal
import com.casadetasha.kexp.petals.migration.`TableMigrations$default_value_petal`
import com.casadetasha.kexp.petals.processor.post.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ExposedBackingAccessorDefaultValueTest: ContainerizedTestBase() {

    private val tableName: String by lazy { `TableMigrations$default_value_petal`().tableName }

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
    fun `store() without provided Int column value stores default value for column`() {
        val baseUuid = UUID.randomUUID()
        val id: UUID = DefaultValuePetal(
            sporeCount = 2,
            color = "Blue",
            secondColor = "Yellow",
            uuid = baseUuid
        ).store().id!!

        val loadedEntity = transaction {
            checkNotNull(DefaultValuePetalEntity.findById(id)) { "Did not find petal $id in DB" }
        }

        assertThat(loadedEntity.id.value).isEqualTo(id)
        assertThat(loadedEntity.count).isEqualTo(10)
        assertThat(loadedEntity.sporeCount).isEqualTo(2)
        assertThat(loadedEntity.color).isEqualTo("Blue")
        assertThat(loadedEntity.secondColor).isEqualTo("Yellow")
        assertThat(loadedEntity.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `store() without provided Long column value stores default value for column`() {
        val baseUuid = UUID.randomUUID()
        val id: UUID = DefaultValuePetal(
            count = 1,
            color = "Blue",
            secondColor = "Yellow",
            uuid = baseUuid
        ).store().id!!

        val loadedEntity = transaction {
            checkNotNull(DefaultValuePetalEntity.findById(id)) { "Did not find petal $id in DB" }
        }

        assertThat(loadedEntity.id.value).isEqualTo(id)
        assertThat(loadedEntity.count).isEqualTo(1)
        assertThat(loadedEntity.sporeCount).isEqualTo(200)
        assertThat(loadedEntity.color).isEqualTo("Blue")
        assertThat(loadedEntity.secondColor).isEqualTo("Yellow")
        assertThat(loadedEntity.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `store() without provided String column value stores default value for column`() {
        val baseUuid = UUID.randomUUID()
        val id: UUID = DefaultValuePetal(
            count = 1,
            sporeCount = 2,
            secondColor = "Yellow",
            uuid = baseUuid
        ).store().id!!

        val loadedEntity = transaction {
            checkNotNull(DefaultValuePetalEntity.findById(id)) { "Did not find petal $id in DB" }
        }

        assertThat(loadedEntity.id.value).isEqualTo(id)
        assertThat(loadedEntity.count).isEqualTo(1)
        assertThat(loadedEntity.sporeCount).isEqualTo(200)
        assertThat(loadedEntity.color).isEqualTo("default color")
        assertThat(loadedEntity.secondColor).isEqualTo("Yellow")
        assertThat(loadedEntity.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `store() without provided @DefaultNull column value stores null value for column`() {
        val baseUuid = UUID.randomUUID()
        val id: UUID = DefaultValuePetal(
            count = 1,
            sporeCount = 2,
            color = "Blue",
            uuid = baseUuid
        ).store().id!!

        val loadedEntity = transaction {
            checkNotNull(DefaultValuePetalEntity.findById(id)) { "Did not find petal $id in DB" }
        }

        assertThat(loadedEntity.id.value).isEqualTo(id)
        assertThat(loadedEntity.count).isEqualTo(1)
        assertThat(loadedEntity.sporeCount).isEqualTo(200)
        assertThat(loadedEntity.color).isEqualTo("Blue")
        assertThat(loadedEntity.secondColor).isNull()
        assertThat(loadedEntity.uuid).isEqualTo(baseUuid)
    }
}
