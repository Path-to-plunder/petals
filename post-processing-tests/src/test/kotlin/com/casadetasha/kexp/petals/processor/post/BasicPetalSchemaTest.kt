package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import kotlin.test.Test

class BasicPetalSchemaTest {

    @Test
    fun `Creates initial version migration`() {
        assertThat(`TableMigrations$basic_petal`().migrateV1())
            .isEqualTo("CREATE TABLE basic_petal ( count INT, name TEXT )")
    }

    @Test
    fun `Creates trailing version migrations`() {
        assertThat(`TableMigrations$basic_petal`().migrateV2())
            .isEqualTo("""
              |ALTER TABLE basic_petal
              |  ADD COLUMN color TEXT,
              |  DROP COLUMN count
              |""".trimMargin())
    }
}
