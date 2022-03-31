package com.casadetasha.kexp.petals.processor.inputparser

import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import javax.lang.model.type.MirroredTypeException

internal class ParsedPetalSchema private constructor(
    val petalSchemaAnnotation: PetalSchema,
    val parsedSchemalessPetal: ParsedSchemalessPetal,
    private val petalSchemaClass: KotlinContainer.KotlinClass,
) {

    val primaryKeyType: PetalPrimaryKey = parsedSchemalessPetal.petalAnnotation.primaryKeyType
    val tableName: String = parsedSchemalessPetal.petalAnnotation.tableName
    val schemaVersion = petalSchemaAnnotation.version

    private lateinit var _parsedPetalColumns: SortedSet<ParsedPetalColumn>
    val parsedPetalColumns: SortedSet<ParsedPetalColumn>
        get() { return _parsedPetalColumns }

    val parsedLocalPetalColumns: SortedSet<LocalPetalColumn> by lazy {
        parsedPetalColumns.filterIsInstance<LocalPetalColumn>().toSortedSet()
    }

    val parsedLocalPetalColumnMap: Map<String, LocalPetalColumn> by lazy {
        parsedLocalPetalColumns.associateBy { it.name }
    }

    var migrationSql: String? = null
    var migrationAlterationSql: List<String>? = null

    fun processMigration(): PetalSchemaMigration {
        return PetalSchemaMigration(
            primaryKeyType = primaryKeyType,
            columnMigrations = parsedLocalPetalColumns.associate { it.name to it.processMigration() }
        ).apply {
            migrationSql = this@ParsedPetalSchema.migrationSql
            migrationAlterationSql = this@ParsedPetalSchema.migrationAlterationSql
        }
    }

    companion object {

        fun parseFromAnnotatedSchemaClass(parsedSchemalessPetals: Map<ClassName, ParsedSchemalessPetal>,
                                          petalSchemaClass: KotlinContainer.KotlinClass
        ): ParsedPetalSchema {
            return parsePetalSchema(parsedSchemalessPetals, petalSchemaClass).apply {
                _parsedPetalColumns = parseColumns(
                    parsedSchemalessPetals = parsedSchemalessPetals,
                    parsedPetalSchema = this)
            }
        }

        private fun parsePetalSchema(parsedSchemalessPetals: Map<ClassName, ParsedSchemalessPetal>,
                                     petalSchemaClass: KotlinContainer.KotlinClass
        ): ParsedPetalSchema {
            val petalSchemaAnnotation: PetalSchema = petalSchemaClass.getAnnotation(PetalSchema::class)!!
            val petalClass = checkNotNull(parsedSchemalessPetals[petalSchemaAnnotation.petalTypeName]) {
                "Parameter \"petal\" for PetalSchema must be a Petal annotated class."
            }

            return ParsedPetalSchema(
                petalSchemaAnnotation = petalSchemaAnnotation,
                petalSchemaClass = petalSchemaClass,
                parsedSchemalessPetal = petalClass
            )
        }

        private fun parseColumns(
            parsedSchemalessPetals: Map<ClassName, ParsedSchemalessPetal>,
            parsedPetalSchema: ParsedPetalSchema,
        ): SortedSet<ParsedPetalColumn> {
            return parsedPetalSchema.petalSchemaClass.kotlinProperties
                .map { ParsedPetalColumn.parsePetalColumn(parsedSchemalessPetals, parsedPetalSchema, it) }
                .toMutableSet()
                .apply {
                    add(PetalIdColumn.parseIdColumn(parsedPetalSchema.parsedSchemalessPetal.petalAnnotation.primaryKeyType))
                }
                .toSortedSet()
        }
    }
}

// asTypeName() should be safe since custom routes will never be Kotlin core classes
@OptIn(DelicateKotlinPoetApi::class)
internal val PetalSchema.petalTypeName: TypeName
    get() = try {
        ClassName(petal.java.packageName, petal.java.simpleName)
    } catch (exception: MirroredTypeException) {
        exception.typeMirror.asTypeName()
    }
