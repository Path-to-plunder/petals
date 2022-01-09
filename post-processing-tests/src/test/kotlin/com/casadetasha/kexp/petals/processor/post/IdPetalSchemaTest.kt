package com.casadetasha.kexp.petals.processor.post

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$int_id_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$long_id_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$no_id_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$uuid_id_petal`
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class IdPetalSchemaTest {

    lateinit var decodedPetalMigration: PetalMigration

    @Test
    fun `Creates table without ID`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$no_id_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE no_id_petal (" +
              " column TEXT NOT NULL" +
              " )"
            )
    }

    @Test
    fun `Creates table with int id`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$int_id_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE int_id_petal (" +
              " id SERIAL AUTO_INCREMENT NOT NULL," +
              " column TEXT NOT NULL" +
              " PRIMARY KEY (id)" +
              " )"
            )
    }
    @Test
    fun `Creates table with long id`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$long_id_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE long_id_petal (" +
              " id BIGSERIAL AUTO_INCREMENT NOT NULL," +
              " column TEXT NOT NULL" +
              " PRIMARY KEY (id)" +
              " )"
            )
    }


    @Test
    fun `Creates table with uuid id`() {
        decodedPetalMigration = Json.decodeFromString(`TableMigrations$uuid_id_petal`().petalJson)
        assertThat(decodedPetalMigration.schemaMigrations[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE uuid_id_petal (" +
              " id uuid NOT NULL," +
              " column TEXT NOT NULL" +
              " PRIMARY KEY (id)" +
              " )"
            )
    }
}
