package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetal
import com.squareup.kotlinpoet.*
import com.zaxxer.hikari.HikariDataSource
import java.io.File

internal class PetalMigrationSetupGenerator(private val migrations: Collection<ParsedPetal>) {

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
                it.classMemberName())
        }

        return builder.build()
    }
}


private fun ParsedPetal.classMemberName(): MemberName {
    val packageName = "com.casadetasha.kexp.petals.migration"
    val className = "TableMigrations\$${petalAnnotation.tableName}"
    return MemberName(packageName, className)
}
