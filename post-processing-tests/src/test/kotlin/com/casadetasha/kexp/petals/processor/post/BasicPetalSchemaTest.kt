package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import com.casadetasha.kexp.petals.processor.PetalMigration
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlin.test.BeforeTest
import kotlin.test.Test

class BasicPetalSchemaTest {

    lateinit var decodedPetalMigration: PetalMigration

    @BeforeTest
    fun setup() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$basic_petal`().petalJson)
    }

    @Test
    fun `Creates table creation migration with all supported types`() {
        assertThat(decodedPetalMigration.schemaMigrations[1]!!.migrationSql)
            .isEqualTo("""
              |CREATE TABLE IF NOT EXISTS basic_petal (
              |  checkingString TEXT NOT NULL,
              |  checkingInt INT NOT NULL,
              |  checkingUUID UUID NOT NULL,
              |  checkingLong BIGINT NOT NULL
              |)
              """.trimMargin())
    }

    @Test
    fun `Creates alter table migration with dropping and adding all supported types`() {
        assertThat(decodedPetalMigration.schemaMigrations[2]!!.migrationSql)
            .isEqualTo("""
              |ALTER TABLE basic_petal
              |  DROP COLUMN checkingString,
              |  DROP COLUMN checkingInt,
              |  DROP COLUMN checkingUUID,
              |  DROP COLUMN checkingLong,
              |  ADD COLUMN color TEXT NOT NULL,
              |  ADD COLUMN count INT NOT NULL,
              |  ADD COLUMN uuid UUID NOT NULL,
              |  ADD COLUMN sporeCount BIGINT NOT NULL
              |""".trimMargin())
    }

    @Test
    fun `Creates alter table migration with renaming of all supported types`() {
        assertThat(decodedPetalMigration.schemaMigrations[3]!!.migrationSql)
            .isEqualTo("""
              |ALTER TABLE basic_petal
              |  RENAME COLUMN count TO renamed_count,
              |  RENAME COLUMN sporeCount TO renamed_sporeCount,
              |  RENAME COLUMN uuid TO renamed_uuid,
              |  RENAME COLUMN color TO renamed_color
              |""".trimMargin())
    }
}
