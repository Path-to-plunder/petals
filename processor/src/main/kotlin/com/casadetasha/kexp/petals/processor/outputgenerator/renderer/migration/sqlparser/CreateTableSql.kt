package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.sqlparser

import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.model.columns.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn

internal class CreatePetalTableSqlParser(private val petalSchema: ParsedPetalSchema): PetalTableSqlParser() {

    val createTableSql: List<String> by lazy {
        val tableCreationSqlRows = mutableListOf("CREATE TABLE \"${petalSchema.tableName}\" (")

        val primaryKeyType = petalSchema.primaryKeyType
        tableCreationSqlRows += when (primaryKeyType) {
            PetalPrimaryKey.INT -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
            PetalPrimaryKey.LONG -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
            else -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
        }

        petalSchema.parsedLocalPetalColumns
            .filterNot { it is PetalIdColumn }
            .forEach {
                tableCreationSqlRows += " ${parseNewColumnSql(it)},"
            }

        tableCreationSqlRows += tableCreationSqlRows.removeLast().removeSuffix(",")

        tableCreationSqlRows += " )"

        return@lazy tableCreationSqlRows
    }
}

internal open class PetalTableSqlParser {

    protected fun parseNewColumnSql(column: LocalPetalColumn): String {
        val notNullExtension = if (!column.isNullable) { " NOT NULL" } else { "" }
        val defaultExtension = when(column is PetalValueColumn && column.hasDefaultValue) {
            true -> " DEFAULT ${column.defaultValue.toDbDefault()}"
            false -> ""
        }
        return "\"${column.name}\" ${column.dataType}$notNullExtension$defaultExtension"
    }
}

internal fun DefaultPetalValue.toDbDefault(): String? {
    return when {
        hasDefaultValue && isString -> "'$value'"
        hasDefaultValue -> "$value"
        else -> null
    }
}
