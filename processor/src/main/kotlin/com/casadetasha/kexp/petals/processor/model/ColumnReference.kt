package com.casadetasha.kexp.petals.processor.model

import com.squareup.kotlinpoet.ClassName

class ColumnReference(
    val kotlinTypeName: ClassName,
    val accessorClassName: ClassName,
    val tableClassName: ClassName,
    val entityClassName: ClassName,
)
