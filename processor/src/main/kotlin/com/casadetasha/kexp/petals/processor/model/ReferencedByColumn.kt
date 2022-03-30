package com.casadetasha.kexp.petals.processor.model

internal data class ReferencedByColumn(
    val columnReference: ColumnReference,
    val columnName: String
)