package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorPropertyTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorPropertyTemplate.Companion.createConstructorPropertyTemplate

internal fun PetalValueColumn.toConstructorPropertyTemplate(): ConstructorPropertyTemplate {
    return createConstructorPropertyTemplate(
        name = name,
        typeName = kotlinType.copy(nullable = isNullable),
        isMutable = true
    ) {
        initializer { CodeTemplate(name) }
    }
}
