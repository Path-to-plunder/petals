package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates

import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.ConstructorPropertyTemplate

internal fun PetalValueColumn.toConstructorPropertyTemplate(): ConstructorPropertyTemplate {
    return ConstructorPropertyTemplate(
        name = name,
        typeName = kotlinType.copy(nullable = isNullable),
        isMutable = true
    ) {
        initializer { CodeTemplate(name) }
    }
}
