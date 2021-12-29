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
    fun `Updates column to nullable if altered column is nullable`() {
        assertThat(`TableMigrations$starting_nullable_petal`().migrateV2())
            .isEqualTo("""
              |ALTER TABLE starting_nullable_petal
              |  ALTER COLUMN color SET NOT NULL
              |""".trimMargin())
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

    @Test
    fun `Updates column to NOT NULL if altered column is non nullable`() {
        assertThat(`TableMigrations$starting_non_nullable_petal`().migrateV2())
            .isEqualTo("""
              |ALTER TABLE starting_non_nullable_petal
              |  ALTER COLUMN color DROP NOT NULL
              |""".trimMargin())
    }
}
