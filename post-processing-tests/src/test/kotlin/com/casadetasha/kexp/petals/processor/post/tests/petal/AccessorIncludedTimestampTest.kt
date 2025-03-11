package com.casadetasha.kexp.petals.processor.post.tests.petal

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.accessor.TimestampPetalData
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$timestamp_petal`
import com.casadetasha.kexp.petals.processor.post.MutableClock
import com.casadetasha.kexp.petals.processor.post.TimestampPetal
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccessorIncludedTimestampTest: ContainerizedTestBase() {

    private lateinit var clock: MutableClock

    private val tableMigration: BasePetalMigration = `TableMigrations$timestamp_petal`()

    private val tableName: String by lazy {
        tableMigration.tableName
    }

    @BeforeTest
    fun setup() {
        tableMigration.migrateToLatest(datasource)
        this.clock = MutableClock(Clock.systemUTC().instant(), ZoneOffset.UTC)
        TimestampPetalData.clock = clock
    }

    @AfterTest
    fun teardown() {
        datasource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM \"$tableName\"").execute()
        }
    }

    @Test
    fun `create() sets createdAt and updatedAt to the current time`() {
        val originalClockTime = clock.instant().toEpochMilli()
        val createdTimestampPetal = TimestampPetalData.create(
            column = "Funzone fun-times"
        )

        clock.advanceBy(Duration.ofSeconds(3))

        val loadedTimestampPetal = transaction {
            checkNotNull(TimestampPetalData.load(createdTimestampPetal.id)) { "Did not find petal $id in DB" }
        }

        assertThat(createdTimestampPetal.createdAt).isEqualTo(originalClockTime)
        assertThat(createdTimestampPetal.updatedAt).isEqualTo(originalClockTime)

        assertThat(loadedTimestampPetal.createdAt).isEqualTo(originalClockTime)
        assertThat(loadedTimestampPetal.updatedAt).isEqualTo(originalClockTime)
    }

    @Test
    fun `store() sets updatedAt to the current time`() {
        val createdTimestampPetal = TimestampPetalData.create(
            column = "Funzone fun-times"
        )

        clock.advanceBy(Duration.ofSeconds(3))
        val updatedTime = clock.instant().toEpochMilli()

        val storedPetalData = TimestampPetalData.store(
            createdTimestampPetal.apply { column = "changed column data" }
        )

        clock.advanceBy(Duration.ofSeconds(5))

        assertThat(storedPetalData.updatedAt).isEqualTo(updatedTime)
    }

    @Test
    fun `store() does NOT set createdAt to the current time`() {
        val originalClockTime = clock.instant().toEpochMilli()
        val createdTimestampPetal = TimestampPetalData.create(
            column = "Funzone fun-times"
        )

        clock.advanceBy(Duration.ofSeconds(3))

        val storedPetalData = TimestampPetalData.store(
            createdTimestampPetal.apply { column = "changed column data" }
        )

        assertThat(storedPetalData.createdAt).isEqualTo(originalClockTime)
    }
}
