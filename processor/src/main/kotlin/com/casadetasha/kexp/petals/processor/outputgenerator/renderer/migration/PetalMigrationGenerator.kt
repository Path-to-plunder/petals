package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate.Companion.generateFile
import com.squareup.kotlinpoet.*
import com.zaxxer.hikari.HikariDataSource

internal class PetalMigrationSetupGenerator(private val migrations: Collection<ParsedPetal>) {

    fun createPetalMigrationSetupClass() {
        generateFile(
            directory = kaptKotlinGeneratedDir,
            packageName = PACKAGE_NAME,
            fileName = CLASS_NAME) {

            generateObject(className = ClassName(PACKAGE_NAME, CLASS_NAME)) {
                generateFunction(name = MIGRATE_METHOD_NAME) {
                    generateParameter("dataSource", HikariDataSource::class.asClassName())

                    generateMethodBody {
                        collectCodeLineTemplates {
                            migrations.map {
                                CodeTemplate( "%M().migrateToLatest(dataSource)", it.classMemberName())
                            }
                        }
                    }
                }
            }
        }.writeToDisk()
    }

    companion object {
        private const val PACKAGE_NAME = "com.casadetasha.kexp.petals"
        private const val CLASS_NAME = "PetalTables"

        private const val MIGRATE_METHOD_NAME = "setupAndMigrateTables"
    }
}

private fun ParsedPetal.classMemberName(): MemberName {
    val packageName = "com.casadetasha.kexp.petals.migration"
    val className = "TableMigrations\$${petalAnnotation.tableName}"
    return MemberName(packageName, className)
}
