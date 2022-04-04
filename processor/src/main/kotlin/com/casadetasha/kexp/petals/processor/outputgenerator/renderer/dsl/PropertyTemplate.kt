package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName

sealed class PropertyTemplate(
    val name: String,
    val typeName: TypeName,
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
        fun KotlinContainerTemplate.collectProperties(function: KotlinContainerTemplate.() -> Collection<PropertyTemplate>) {
            addProperties(function())
        }
    }
}

class ConstructorPropertyTemplate(
    name: String,
    typeName: TypeName,
    isMutable: Boolean? = null,
    annotations: Collection<AnnotationTemplate>? = null
): PropertyTemplate(
    name = name,
    typeName = typeName,
    isMutable =  isMutable,
    annotations = annotations,
    isParameter = true
) {

    companion object {
        fun ConstructorTemplate.collectConstructorProperties(classTemplate: ClassTemplate, function: ConstructorTemplate.() -> Collection<ConstructorPropertyTemplate>) {
            addConstructorProperties(classTemplate, this.function())
        }
    }
}
