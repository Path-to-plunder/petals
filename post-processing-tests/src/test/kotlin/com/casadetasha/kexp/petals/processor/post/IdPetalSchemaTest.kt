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
              |CREATE TABLE no_id_petal (
              |  column TEXT NOT NULL
              |)
              """.trimMargin())
    }

    @Test
    fun `Creates table with int autoincrement id`() {
        assertThat(`TableMigrations$int_autoincrement_id_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE int_autoincrement_id_petal (
              |  column id INT AUTO_INCREMENT NOT NULL,
              |  column TEXT NOT NULL
              |  PRIMARY KEY (id)
              |)
              """.trimMargin())
    }

    @Test
    fun `Creates table with int id`() {
        assertThat(`TableMigrations$int_id_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE int_id_petal (
              |  column id INT NOT NULL,
              |  column TEXT NOT NULL
              |  PRIMARY KEY (id)
              |)
              """.trimMargin())
    }
    @Test
    fun `Creates table with text id`() {
        assertThat(`TableMigrations$text_id_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE text_id_petal (
              |  column id TEXT NOT NULL,
              |  column TEXT NOT NULL
              |  PRIMARY KEY (id)
              |)
              """.trimMargin())
    }


    @Test
    fun `Creates table with uuid id`() {
        assertThat(`TableMigrations$uuid_id_petal`().migrateV1())
            .isEqualTo("""
              |CREATE TABLE uuid_id_petal (
              |  column id uuid NOT NULL,
              |  column TEXT NOT NULL
              |  PRIMARY KEY (id)
              |)
              """.trimMargin())
    }
}
