package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.migration.`TableMigrations$create_as_nullable_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$nullability_petal`
import kotlin.test.Test

class NullabilityPetalSchemaTest {

    @Test
    fun `Creates column as nullable if schema property is nullable`() {
        assertThat(`TableMigrations$create_as_nullable_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE create_as_nullable_petal (
              |  color TEXT
              |)
              """.trimMargin())
    }

    @Test
    fun `Creates column as NOT NULL if schema property is not nullable`() {
        assertThat(`TableMigrations$nullability_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE nullability_petal (
              |  color TEXT NOT NULL
              |)
              """.trimMargin())
    }

//    @Test
//    fun `Creates alter table migration with dropping and adding all supported types`() {
//        assertThat(`TableMigrations$nullability_petal`().migrateV2())
//            .isEqualTo("""
//              |ALTER TABLE nullability_petal
//              |  DROP COLUMN checkingLong,
//              |  DROP COLUMN checkingString,
//              |  DROP COLUMN checkingUUID,
//              |  DROP COLUMN checkingInt,
//              |  ADD COLUMN sporeCount BIGINT,
//              |  ADD COLUMN uuid UUID,
//              |  ADD COLUMN count INT,
//              |  ADD COLUMN color TEXT
//              |""".trimMargin())
//    }
}
