package com.casadetasha.kexp.petals.processor.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.processor.UnprocessedPetalMigration
import com.squareup.kotlinpoet.*
import com.zaxxer.hikari.HikariDataSource
import java.io.File

internal class PetalMigrationSetupGenerator(private val migrations: MutableCollection<UnprocessedPetalMigration>) {

    fun createPetalMigrationSetupClass() {
        FileSpec.builder("com.casadetasha.kexp.petals", "PetalTables")
            .addType(
                TypeSpec.objectBuilder("PetalTables")
                    .addFunction(createPetalMigrationsFunction())
                    .build()
            )
            .build()
            .writeTo(File(kaptKotlinGeneratedDir))
    }

    private fun createPetalMigrationsFunction(): FunSpec {
        val builder = FunSpec.builder("setupAndMigrateTables")
            .addParameter(
                ParameterSpec.builder("dataSource", HikariDataSource::class)
                .build()
            )

        migrations.forEach {
            builder.addStatement("%M().migrateToLatest(dataSource)",
                it.classMemberName)
        }

        return builder.build()
    }
}
