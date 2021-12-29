package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.migration.`TableMigrations$starting_nullable_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$starting_non_nullable_petal`
import kotlin.test.Test

class NullabilityPetalSchemaTest {

    @Test
    fun `Creates column as nullable if schema property is nullable`() {
        assertThat(`TableMigrations$starting_nullable_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE starting_nullable_petal (
              |  color TEXT
              |)
              """.trimMargin())
    }

    @Test
    fun `Creates column as NOT NULL if schema property is not nullable`() {
        assertThat(`TableMigrations$starting_non_nullable_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE starting_non_nullable_petal (
              |  color TEXT NOT NULL
              |)
              """.trimMargin())
    }

//    @Test
//    fun `Creates alter table migration with dropping and adding all supported types`() {
//        assertThat(`TableMigrations$starting_non_nullable_petal`().migrateV2())
//            .isEqualTo("""
//              |ALTER TABLE starting_non_nullable_petal
//              |  ALTER COLUMN color TEXT SET NOT NULL
//              |""".trimMargin())
//    }
}
