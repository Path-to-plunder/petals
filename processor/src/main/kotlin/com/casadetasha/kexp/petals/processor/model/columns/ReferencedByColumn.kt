package com.casadetasha.kexp.petals.processor.model.columns

internal data class ReferencedByColumn(
    val columnReference: ColumnReference,
    val columnName: String
)