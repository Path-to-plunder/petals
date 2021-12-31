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
              |CREATE TABLE IF NOT EXISTS starting_nullable_petal (
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
    fun `Added nullable columns are added as nullable`() {
        assertThat(`TableMigrations$starting_nullable_petal`().migrateV3())
            .isEqualTo("""
              |ALTER TABLE starting_nullable_petal
              |  ADD COLUMN secondColor TEXT
              |""".trimMargin())
    }

    @Test
    fun `Creates column as NOT NULL if schema property is not nullable`() {
        assertThat(`TableMigrations$starting_non_nullable_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE IF NOT EXISTS starting_non_nullable_petal (
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

    @Test
    fun `Added non nullable columns are added as non nullable`() {
        assertThat(`TableMigrations$starting_non_nullable_petal`().migrateV3())
            .isEqualTo("""
              |ALTER TABLE starting_non_nullable_petal
              |  ADD COLUMN secondColor TEXT NOT NULL
              |""".trimMargin())
    }
}
