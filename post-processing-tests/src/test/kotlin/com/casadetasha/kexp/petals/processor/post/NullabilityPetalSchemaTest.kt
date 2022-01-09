package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$starting_non_nullable_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$starting_nullable_petal`
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class NullabilityPetalSchemaTest {

    lateinit var decodedPetalMigration: PetalMigration

    @Test
    fun `Creates column as nullable if schema property is nullable`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$starting_nullable_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[1]!!.migrationSql)
            .isEqualTo("""
              |CREATE TABLE starting_nullable_petal (
              |  color TEXT
              |)
              """.trimMargin())
    }

    @Test
    fun `Updates column to nullable if altered column is nullable`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$starting_nullable_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[2]!!.migrationSql)
            .isEqualTo("""
              |ALTER TABLE starting_nullable_petal
              |  ALTER COLUMN color SET NOT NULL
              |""".trimMargin())
    }

    @Test
    fun `Added nullable columns are added as nullable`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$starting_nullable_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[3]!!.migrationSql)
            .isEqualTo("""
              |ALTER TABLE starting_nullable_petal
              |  ADD COLUMN secondColor TEXT
              |""".trimMargin())
    }

    @Test
    fun `Creates column as NOT NULL if schema property is not nullable`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$starting_non_nullable_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[1]!!.migrationSql)
            .isEqualTo("""
              |CREATE TABLE starting_non_nullable_petal (
              |  color TEXT NOT NULL
              |)
              """.trimMargin())
    }

    @Test
    fun `Updates column to NOT NULL if altered column is non nullable`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$starting_non_nullable_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[2]!!.migrationSql)
            .isEqualTo("""
              |ALTER TABLE starting_non_nullable_petal
              |  ALTER COLUMN color DROP NOT NULL
              |""".trimMargin())
    }

    @Test
    fun `Added non nullable columns are added as non nullable`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$starting_non_nullable_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[3]!!.migrationSql)
            .isEqualTo("""
              |ALTER TABLE starting_non_nullable_petal
              |  ADD COLUMN secondColor TEXT NOT NULL
              |""".trimMargin())
    }
}
