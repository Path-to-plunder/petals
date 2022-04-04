package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName

class PropertyTemplate(
    name: String,
    typeName: TypeName,
    isMutable: Boolean? = null,
    annotations: Collection<AnnotationTemplate>? = null,
    isParameter: Boolean = false
) {

    private val propertyBuilder = PropertySpec.builder(name, typeName)
    internal val propertySpec: PropertySpec

    init {
        annotations?.let { propertyBuilder.addAnnotations(annotations.map { it.annotationSpec } ) }

        isMutable?.let { propertyBuilder.mutable() }

        if (isParameter) { propertyBuilder.initializer(name) }

        propertySpec = propertyBuilder.build()
    }

    companion object {
        fun ClassTemplate.collectProperties(function: ClassTemplate.() -> Collection<PropertyTemplate>) {
            addProperties(function())
        }
    }
}
