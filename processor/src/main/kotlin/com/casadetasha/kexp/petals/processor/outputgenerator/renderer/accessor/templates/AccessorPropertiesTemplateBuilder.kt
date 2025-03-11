package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates

import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.ConstructorPropertyTemplate
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn

internal fun LocalPetalColumn.toConstructorPropertyTemplate(): ConstructorPropertyTemplate {
    return ConstructorPropertyTemplate(
        name = name,
        typeName = kotlinType.copy(nullable = isNullable),
        isMutable = isMutable
    ) {
        initializer { CodeTemplate(name) }
    }
}
