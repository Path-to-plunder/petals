package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.ConstructorPropertyTemplate
import com.casadetasha.kexp.generationdsl.dsl.ConstructorPropertyTemplate.Companion.createConstructorPropertyTemplate

internal fun PetalValueColumn.toConstructorPropertyTemplate(): ConstructorPropertyTemplate {
    return createConstructorPropertyTemplate(
        name = name,
        typeName = kotlinType.copy(nullable = isNullable),
        isMutable = true
    ) {
        initializer { CodeTemplate(name) }
    }
}
