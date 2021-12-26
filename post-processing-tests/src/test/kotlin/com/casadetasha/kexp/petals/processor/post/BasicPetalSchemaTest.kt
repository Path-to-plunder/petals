package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import kotlin.test.Test

class BasicPetalSchemaTest {

    @Test
    fun `Creates table creation migration with all supported types`() {
        assertThat(`TableMigrations$basic_petal`().migrateV1())
            .isEqualTo(
                "CREATE TABLE basic_petal ( checkingLong BIGINT, checkingString TEXT, checkingUUID UUID, checkingInt INT )")
    }

    @Test
    fun `Creates alter table migration with all supported types`() {
        assertThat(`TableMigrations$basic_petal`().migrateV2())
            .isEqualTo("""
              |ALTER TABLE basic_petal
              |  DROP COLUMN checkingLong,
              |  DROP COLUMN checkingString,
              |  DROP COLUMN checkingUUID,
              |  DROP COLUMN checkingInt,
              |  ADD COLUMN sporeCount BIGINT,
              |  ADD COLUMN uuid UUID,
              |  ADD COLUMN count INT,
              |  ADD COLUMN color TEXT
              |""".trimMargin())
    }
}
