package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetal
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetalSchema
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class MigrationClassSpecParser(private val petal: ParsedPetal, className: String) {

    val petalMigrationClassSpec: TypeSpec by lazy {
        addMigrationSpecs()
        TypeSpec.classBuilder(className).superclass(BasePetalMigration::class)
            .addOverriddenStringProperty(
                "petalJson",
                json.encodeToString(petal.processMigration()
                )
            ).build()
    }

    private fun addMigrationSpecs() {
        var previousSchema: ParsedPetalSchema? = null
        petal.schemas.forEach { (_, currentSchema) ->
            when(val nullCheckedPreviousSchema = previousSchema) {
                null -> setCreateTableSqlForSchema(currentSchema)
                else -> setMigrationsForSchema(nullCheckedPreviousSchema, currentSchema)
            }

            previousSchema = currentSchema
        }
    }

    private fun setCreateTableSqlForSchema(schema: ParsedPetalSchema) {
        schema.migrationSql = CreatePetalTableSqlParser(schema).createTableSql
    }

    private fun setMigrationsForSchema(previousSchema: ParsedPetalSchema, currentSchema: ParsedPetalSchema) {
        val migrationParser = MigratePetalTableSqlParser(previousSchema, currentSchema)
        currentSchema.migrationSql = migrationParser.migrateTableSql
        currentSchema.migrationAlterationSql = migrationParser.renameSql
    }

    private fun TypeSpec.Builder.addOverriddenStringProperty(propertyName: String, propertyValue: String) = apply {
        addProperty(
            PropertySpec.builder(propertyName, String::class)
                .initializer(
                    CodeBlock.builder()
                        .add("%S", propertyValue)
                        .build()
                ).addModifiers(KModifier.OVERRIDE)
                .build()
        )
    }

    companion object {

        private val json = Json { prettyPrint = true }
    }
}
