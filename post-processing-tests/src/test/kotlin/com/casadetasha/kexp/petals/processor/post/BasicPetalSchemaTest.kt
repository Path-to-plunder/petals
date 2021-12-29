package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import kotlin.test.Test

class BasicPetalSchemaTest {

    @Test
    fun `Creates table creation migration with all supported types`() {
        assertThat(`TableMigrations$basic_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE basic_petal (
              |  checkingString TEXT NOT NULL,
              |  checkingInt INT NOT NULL,
              |  checkingUUID UUID NOT NULL,
              |  checkingLong BIGINT NOT NULL
              |)
              """.trimMargin())
    }

    @Test
    fun `Creates alter table migration with dropping and adding all supported types`() {
        assertThat(`TableMigrations$basic_petal`().migrateV2())
            .isEqualTo("""
              |ALTER TABLE basic_petal
              |  DROP COLUMN checkingString,
              |  DROP COLUMN checkingInt,
              |  DROP COLUMN checkingUUID,
              |  DROP COLUMN checkingLong,
              |  ADD COLUMN color TEXT,
              |  ADD COLUMN count INT,
              |  ADD COLUMN uuid UUID,
              |  ADD COLUMN sporeCount BIGINT
              |""".trimMargin())
    }

    @Test
    fun `Creates alter table migration with renaming of all supported types`() {
        assertThat(`TableMigrations$basic_petal`().migrateV3())
            .isEqualTo("""
              |ALTER TABLE basic_petal
              |  RENAME COLUMN count TO renamed_count,
              |  RENAME COLUMN sporeCount TO renamed_sporeCount,
              |  RENAME COLUMN uuid TO renamed_uuid,
              |  RENAME COLUMN color TO renamed_color
              |""".trimMargin())
    }
}
