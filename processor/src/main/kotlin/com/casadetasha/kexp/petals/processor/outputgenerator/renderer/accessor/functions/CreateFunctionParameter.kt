package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

internal fun LocalPetalColumn.asCreateFunctionParameterTemplate(): ParameterTemplate {
    val propertyTypeName = when (this is PetalIdColumn) {
        true -> kotlinType.copy(nullable = true)
        false -> getNonIdTypeName()
    }

    return ParameterTemplate(name = name, typeName = propertyTypeName) {
        when (this@asCreateFunctionParameterTemplate) {
            is PetalIdColumn -> defaultValue { CodeTemplate("null") }
            is PetalValueColumn -> columnDefaultValue(defaultValue)
            is PetalReferenceColumn -> if (isNullable) {
                defaultValue { CodeTemplate("null") }
            }
        }
    }
}

private fun ParameterTemplate.columnDefaultValue(defaultValue: DefaultPetalValue) {
    if (!defaultValue.hasDefaultValue) return

    val defaultCodeTemplate = when (defaultValue.typeName.copy(nullable = false)) {
        String::class.asClassName() -> CodeTemplate("%S", defaultValue.value)
        else -> CodeTemplate("%L", defaultValue.value)
    }

    defaultValue { defaultCodeTemplate }
}

private fun LocalPetalColumn.getNonIdTypeName(): TypeName {
    return when (this) {
        is PetalReferenceColumn -> referencingAccessorClassName.copy(nullable = isNullable)
        else -> kotlinType.copy(nullable = isNullable)
    }
}
