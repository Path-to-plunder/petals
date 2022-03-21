package com.casadetasha.kexp.petals.processor.migration

import com.squareup.kotlinpoet.ClassName

class ColumnReference(
    var accessorClassName: ClassName,
    var entityClassName: ClassName,
)
