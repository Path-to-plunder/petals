package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class BasicPetalSchemaTest {

    @Test
    fun `Creates PetalMigration File`() {
        assertThat(BasicPetalSchemaMigration().migrate())
            .isEqualTo("CREATE TABLE basic_petal ( count INT, name TEXT )")
    }
}