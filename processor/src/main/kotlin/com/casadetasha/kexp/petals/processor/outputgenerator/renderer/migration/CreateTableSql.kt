package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.processor.inputparser.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.inputparser.PetalIdColumn

internal class CreatePetalTableSqlParser(val petalSchema: ParsedPetalSchema): PetalTableSqlParser() {

    val createTableSql: String by lazy {
        var tableCreationSql = "CREATE TABLE \"${petalSchema.tableName}\" ("

        val primaryKeyType = petalSchema.primaryKeyType
        tableCreationSql += when (primaryKeyType) {
            PetalPrimaryKey.INT -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
            PetalPrimaryKey.LONG -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
            else -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
        }

        petalSchema.parsedLocalPetalColumns
            .filterNot { it is PetalIdColumn }
            .forEach {
                tableCreationSql += " ${parseNewColumnSql(it)},"
            }
        tableCreationSql = tableCreationSql.removeSuffix(",")

        tableCreationSql += " )"

        return@lazy tableCreationSql
    }
}

internal open class PetalTableSqlParser {

    protected fun parseNewColumnSql(column: LocalPetalColumn): String {
        var sql = "\"${column.name}\" ${column.dataType}"
        if (!column.isNullable) {
            sql += " NOT NULL"
        }

        return sql
    }
}
