package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate

internal fun PetalReferenceColumn.asParameterTemplate(): ParameterTemplate =
    ParameterTemplate(
        name = "${name}Id",
        typeName = kotlinType.copy(nullable = isNullable)
    ) {
        if (isNullable) {
            defaultValue { CodeTemplate("null") }
        }
    }
