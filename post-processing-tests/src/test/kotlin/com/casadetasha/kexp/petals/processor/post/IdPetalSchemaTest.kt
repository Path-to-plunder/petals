package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.migration.*
import kotlin.test.Test

class IdPetalSchemaTest {

    @Test
    fun `Creates table without ID`() {
        assertThat(`TableMigrations$no_id_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE IF NOT EXISTS no_id_petal (
              |  column TEXT NOT NULL
              |)
              """.trimMargin())
    }

    @Test
    fun `Creates table with int id`() {
        assertThat(`TableMigrations$int_id_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE IF NOT EXISTS int_id_petal (
              |  id SERIAL AUTO_INCREMENT NOT NULL,
              |  column TEXT NOT NULL
              |  PRIMARY KEY (id)
              |)
              """.trimMargin())
    }
    @Test
    fun `Creates table with long id`() {
        assertThat(`TableMigrations$text_id_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE IF NOT EXISTS long_id_petal (
              |  id BIGSERIAL AUTO_INCREMENT NOT NULL,
              |  column TEXT NOT NULL
              |  PRIMARY KEY (id)
              |)
              """.trimMargin())
    }


    @Test
    fun `Creates table with uuid id`() {
        assertThat(`TableMigrations$uuid_id_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE IF NOT EXISTS uuid_id_petal (
              |  id uuid NOT NULL,
              |  column TEXT NOT NULL
              |  PRIMARY KEY (id)
              |)
              """.trimMargin())
    }
}
