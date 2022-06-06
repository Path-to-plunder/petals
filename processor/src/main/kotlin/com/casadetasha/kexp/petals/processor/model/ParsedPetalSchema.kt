package com.casadetasha.kexp.petals.processor.model

import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.PetalSchema
import com.casadetasha.kexp.petals.annotations.ExecuteSqlBeforeMigration
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.ParsedPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import javax.lang.model.type.MirroredTypeException

internal class ParsedPetalSchema private constructor(
    val petalSchemaAnnotation: PetalSchema,
    val runBeforeMigrationAnnotation: ExecuteSqlBeforeMigration?,
    val parsedSchemalessPetal: ParsedSchemalessPetal,
    private val petalSchemaClass: KotlinContainer.KotlinClass,
) {

    val preMigrationSql: String? = runBeforeMigrationAnnotation?.executableSql
    val baseClassName: String = parsedSchemalessPetal.baseSimpleName
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

    companion object {

        fun parseWithAnnotatedSchemaClass(parsedSchemalessPetals: Map<ClassName, ParsedSchemalessPetal>,
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
                "INTERNAL LIBRARY ERROR: Parameter \"petal\" for PetalSchema must be a Petal annotated class."
            }

            return ParsedPetalSchema(
                petalSchemaAnnotation = petalSchemaAnnotation,
                runBeforeMigrationAnnotation = petalSchemaClass.getAnnotation(ExecuteSqlBeforeMigration::class),
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
                    add(PetalIdColumn.parseIdColumn(
                        parsedPetalSchema,
                        parsedPetalSchema.primaryKeyType))
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
