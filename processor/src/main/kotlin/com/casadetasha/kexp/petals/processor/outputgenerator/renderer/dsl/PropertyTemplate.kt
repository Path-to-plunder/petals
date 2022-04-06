package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName

open class PropertyTemplate(
    val name: String,
    val typeName: TypeName,
    isMutable: Boolean? = null,
    annotations: Collection<AnnotationTemplate>? = null,
    function: (PropertyTemplate.() -> Unit)? = null
) {

    private var _initializer: CodeTemplate? = null
    private var _delegate: CodeTemplate? = null

    private val propertyBuilder = PropertySpec.builder(name, typeName)

    internal val propertySpec: PropertySpec by lazy {
        function?.let { this.function() }

        annotations?.let { propertyBuilder.addAnnotations(annotations.map { it.annotationSpec } ) }
        isMutable?.let { propertyBuilder.mutable() }
        _initializer?.let { propertyBuilder.initializer(it.codeBlock) }
        _delegate?.let { propertyBuilder.delegate(it.codeBlock) }

        propertyBuilder.build()
    }

    fun initializer(function: () -> CodeTemplate) {
        _initializer = function()
    }

    protected fun setInitializer(initializerBlock: String) {
        _initializer = CodeTemplate(initializerBlock)
    }

    fun delegate(function: () -> CodeTemplate) {
        _delegate = function()
    }

    companion object {
        fun KotlinContainerTemplate.collectProperties(function: KotlinContainerTemplate.() -> Collection<PropertyTemplate>) {
            addProperties(function())
        }

        fun createPropertyTemplate(
            name: String,
            typeName: TypeName,
            isMutable: Boolean? = null,
            annotations: Collection<AnnotationTemplate>? = null,
            function: (PropertyTemplate.() -> Unit)?
        ): PropertyTemplate {
            return PropertyTemplate(
                name = name,
                typeName = typeName,
                isMutable =  isMutable,
                annotations = annotations,
                function = function,
            )
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
) {

    init {
        setInitializer(name)
    }

    companion object {
        fun ConstructorTemplate.collectConstructorProperties(classTemplate: ClassTemplate, function: ConstructorTemplate.() -> Collection<ConstructorPropertyTemplate>) {
            addConstructorProperties(classTemplate, this.function())
        }
    }
}
