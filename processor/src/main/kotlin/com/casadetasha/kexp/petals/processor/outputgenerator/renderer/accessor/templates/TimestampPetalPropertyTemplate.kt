package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates

import com.casadetasha.kexp.generationdsl.dsl.KotlinModifiers
import com.casadetasha.kexp.generationdsl.dsl.PropertyTemplate
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.model.columns.PetalTimestampColumn

internal fun createTimestampPropertyTemplates(accessorClassInfo: AccessorClassInfo): List<PropertyTemplate> {
    return accessorClassInfo.petalColumns
        .filterIsInstance<PetalTimestampColumn>()
        .map {
            PropertyTemplate(name = it.name, typeName = it.kotlinType, isMutable = it.isMutable) {
                visibility { KotlinModifiers.Visibility.PUBLIC }
            }
        }
}
