package com.casadetasha.kexp.petals.processor.model.columns

import com.squareup.kotlinpoet.ClassName

internal data class ColumnReference(
    val kotlinTypeName: ClassName,
    val accessorClassName: ClassName,
    val tableClassName: ClassName,
    val entityClassName: ClassName,
)
