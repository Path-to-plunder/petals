package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.model.columns.*
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.ParsedPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.asClassName
import java.util.*

internal class AccessorClassInfo(
    packageName: String,
    simpleName: String,
    val petalColumns: SortedSet<ParsedPetalColumn>,
    val entityClassName: ClassName,
    val tableClassName: ClassName,
    val dataClassName: ClassName
    ) {

    val localColumns: SortedSet<LocalPetalColumn> = petalColumns.filterIsInstance<LocalPetalColumn>().toSortedSet()
    val petalValueColumns: SortedSet<PetalValueColumn> = localColumns.filterIsInstance<PetalValueColumn>().toSortedSet()
    val petalReferenceColumns: SortedSet<PetalReferenceColumn> =
        localColumns.filterIsInstance<PetalReferenceColumn>().toSortedSet()

    val idKotlinClassName: ClassName by lazy {
        val idColumn: PetalIdColumn? = petalColumns.firstOrNull { it is PetalIdColumn } as PetalIdColumn?
        idColumn?.kotlinType ?: Int::class.asClassName()
    }
    val className: ClassName = ClassName(packageName, simpleName)

    val entityMemberName by lazy { MemberName(entityClassName.packageName, entityClassName.simpleName) }
    val tableMemberName by lazy { MemberName(tableClassName.packageName, tableClassName.simpleName) }
}

