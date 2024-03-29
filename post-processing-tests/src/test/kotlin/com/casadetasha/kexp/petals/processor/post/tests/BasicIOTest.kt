package com.casadetasha.kexp.petals.processor.post.tests

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.accessor.BasicPetal
import com.casadetasha.kexp.petals.accessor.BasicPetal.Companion.delete
import com.casadetasha.kexp.petals.accessor.BasicPetal.Companion.store
import com.casadetasha.kexp.petals.accessor.toPetal
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class BasicIOTest: ContainerizedTestBase() {

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
    fun `Exports to accessor`() {
        val baseUuid = UUID.randomUUID()
        val petalEntity: BasicPetal = transaction {
            BasicPetalEntity.new {
                count = 1
                sporeCount = 2
                color = "Blue"
                secondColor = "Yellow"
                uuid = baseUuid
            }.toPetal()
        }

        assertThat(petalEntity.count).isEqualTo(1)
    }

    @Test
    fun `load() loads stored data`() {
        val baseUuid = UUID.randomUUID()
        val petalId: UUID = transaction {
            BasicPetalEntity.new {
                count = 1
                sporeCount = 2
                color = "Blue"
                secondColor = "Yellow"
                uuid = baseUuid
            }.id.value
        }

        val loadedPetal = checkNotNull(BasicPetal.load(petalId))
        assertThat(loadedPetal.id).isEqualTo(petalId)
        assertThat(loadedPetal.count).isEqualTo(1)
        assertThat(loadedPetal.sporeCount).isEqualTo(2)
        assertThat(loadedPetal.color).isEqualTo("Blue")
        assertThat(loadedPetal.secondColor).isEqualTo("Yellow")
        assertThat(loadedPetal.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `loadAll() loads all created objects`() {
        transaction {
            repeat(times = 10) { petalNumber ->
                BasicPetalEntity.new {
                    count = petalNumber
                    sporeCount = 2
                    color = "Blue"
                    secondColor = "Yellow"
                    uuid = UUID.randomUUID()
                }
            }
        }

        val petalList: List<BasicPetal> = BasicPetal.loadAll()

        assertThat(petalList.size).isEqualTo(10)
        repeat(times = petalList.size) { petalIndex ->
            assertThat(petalList[petalIndex].count).isEqualTo(petalIndex)
        }
    }

    @Test
    fun `lazyLoadAll() loads all created objects`() {
        transaction {
            repeat(times = 10) { petalNumber ->
                BasicPetalEntity.new {
                    count = petalNumber
                    sporeCount = 2
                    color = "Blue"
                    secondColor = "Yellow"
                    uuid = UUID.randomUUID()
                }
            }
        }

        val petalIterable: SizedIterable<BasicPetal> = BasicPetal.lazyLoadAll()
        val petalList = transaction { petalIterable.toList() }

        assertThat(petalList.size).isEqualTo(10)
        repeat(times = 10) { petalIndex ->
            assertThat(petalList[petalIndex].count).isEqualTo(petalIndex)
        }
    }

    @Test
    fun `store() without provided ID stores data with generated ID`() {
        val baseUuid = UUID.randomUUID()
        val id: UUID = BasicPetal.create(
            count = 1,
            sporeCount = 2,
            color = "Blue",
            secondColor = "Yellow",
            uuid = baseUuid
        ).id

        val loadedEntity = transaction {
            checkNotNull(BasicPetalEntity.findById(id))
        }

        assertThat(loadedEntity.id.value).isEqualTo(id)
        assertThat(loadedEntity.count).isEqualTo(1)
        assertThat(loadedEntity.sporeCount).isEqualTo(2)
        assertThat(loadedEntity.color).isEqualTo("Blue")
        assertThat(loadedEntity.secondColor).isEqualTo("Yellow")
        assertThat(loadedEntity.uuid).isEqualTo(baseUuid)
    }

    @Test
    fun `store() with provided ID stores data`() {
        val recordUuid = UUID.randomUUID()
        val secondUuid = UUID.randomUUID()
        BasicPetal.create(
            id = recordUuid,
            count = 1,
            sporeCount = 2,
            color = "Blue",
            secondColor = "Yellow",
            uuid = secondUuid
        )

        val loadedEntity = transaction {
             checkNotNull(BasicPetalEntity.findById(recordUuid))
        }

        assertThat(loadedEntity.id.value).isEqualTo(recordUuid)
        assertThat(loadedEntity.count).isEqualTo(1)
        assertThat(loadedEntity.sporeCount).isEqualTo(2)
        assertThat(loadedEntity.color).isEqualTo("Blue")
        assertThat(loadedEntity.secondColor).isEqualTo("Yellow")
        assertThat(loadedEntity.uuid).isEqualTo(secondUuid)
    }

    @Test
    fun `changing values on loaded data without calling store() does not update data`() {
        val baseUuid = UUID.randomUUID()
        val petalId: UUID = transaction {
            BasicPetalEntity.new {
                count = 1
                sporeCount = 2
                color = "Blue"
                secondColor = "Yellow"
                uuid = baseUuid
            }.id.value
        }

        val loadedPetal = transaction {
            checkNotNull(BasicPetalEntity.findById(petalId))
        }.toPetal()

        loadedPetal.color = "Orange"

        val reloadedPetalEntity = transaction {
            checkNotNull(BasicPetalEntity.findById(petalId))
        }

        assertThat(reloadedPetalEntity.color).isEqualTo("Blue")
    }

    @Test
    fun `calling store() after changing values on loaded data updates data`() {
        val baseUuid = UUID.randomUUID()
        val petalId: UUID = transaction {
            BasicPetalEntity.new {
                count = 1
                sporeCount = 2
                color = "Blue"
                secondColor = "Yellow"
                uuid = baseUuid
            }.id.value
        }

        val loadedPetal = transaction {
            checkNotNull(BasicPetalEntity.findById(petalId))
        }.toPetal()

        loadedPetal.color = "Orange"
        store(loadedPetal)

        val reloadedPetalEntity = transaction {
            checkNotNull(BasicPetalEntity.findById(petalId))
        }

        assertThat(reloadedPetalEntity.color).isEqualTo("Orange")
    }

    @Test
    fun `calling delete() on stored data deletes the data`() {
        val baseUuid = UUID.randomUUID()
        val petalId: UUID = transaction {
            BasicPetalEntity.new {
                count = 1
                sporeCount = 2
                color = "Blue"
                secondColor = "Yellow"
                uuid = baseUuid
            }.id.value
        }

        val loadedPetal = transaction {
            checkNotNull(BasicPetalEntity.findById(petalId))
        }.toPetal()

        delete(loadedPetal)

        val reloadedPetalEntity = transaction {
            BasicPetalEntity.findById(petalId)
        }

        assertThat(reloadedPetalEntity).isNull()
    }

    @Test
    fun `calling delete() on non stored data does nothing`() {
        val baseUuid = UUID.randomUUID()
        val petal = BasicPetal.create(
            count = 1,
            sporeCount = 2,
            color = "Blue",
            secondColor = "Yellow",
            uuid = baseUuid,
        )

        delete(petal)
    }
}
