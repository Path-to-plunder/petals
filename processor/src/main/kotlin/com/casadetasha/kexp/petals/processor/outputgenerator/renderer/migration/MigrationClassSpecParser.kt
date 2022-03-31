package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.casadetasha.kexp.petals.processor.model.ParsedPetalSchema
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class MigrationClassSpecParser(private val petal: ParsedPetal, className: String) {

    val petalMigrationClassSpec: TypeSpec by lazy {
        TypeSpec.classBuilder(className).superclass(BasePetalMigration::class)
            .addOverriddenStringProperty(
                propertyName = "petalJson",
                propertyValue = json.encodeToString(parsePetalMigration(petal))
            ).build()
    }

    private fun parsePetalMigration(petal: ParsedPetal): PetalMigration {
        return PetalMigration(
            tableName = petal.tableName,
            className = petal.baseSimpleName,
            schemaMigrations = parsePetalMigrationSchemas()
        )
    }

    private fun parsePetalMigrationSchemas(): Map<Int, PetalSchemaMigration> {
        val migrationSchemas = HashMap<Int, PetalSchemaMigration>()

        var previousSchema: ParsedPetalSchema? = null
        petal.schemas.values.forEach { currentSchema ->
            migrationSchemas[currentSchema.schemaVersion] =
                when(val nullCheckedPreviousSchema = previousSchema) {
                    null -> createSchemaForCreateTable(currentSchema)
                    else -> createSchemaForTableMigration(nullCheckedPreviousSchema, currentSchema)
                }

            previousSchema = currentSchema
        }

        return migrationSchemas
    }

    private fun createSchemaForCreateTable(schema: ParsedPetalSchema): PetalSchemaMigration {
        return PetalSchemaMigration(
            primaryKeyType = schema.primaryKeyType,
            columnMap = schema.parsedLocalPetalColumns
                .map {
                    PetalColumn(
                        name = it.name,
                        dataType = it.dataType,
                        isNullable = it.isNullable
                    )
                }
                .associateBy { it.name },
            migrationSqlRows = CreatePetalTableSqlParser(schema).createTableSql
        )
    }

    private fun createSchemaForTableMigration(previousSchema: ParsedPetalSchema, currentSchema: ParsedPetalSchema): PetalSchemaMigration {
        val migrationParser = MigratePetalTableSqlParser(previousSchema, currentSchema)

        return PetalSchemaMigration(
            primaryKeyType = currentSchema.primaryKeyType,
            columnMap = currentSchema.parsedLocalPetalColumns
                .map {
                    PetalColumn(
                        name = it.name,
                        dataType = it.dataType,
                        isNullable = it.isNullable
                    )
                }
                .associateBy { it.name },
            migrationSqlRows = migrationParser.migrateTableSql,
            migrationAlterationSql = migrationParser.renameSql
        )
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
