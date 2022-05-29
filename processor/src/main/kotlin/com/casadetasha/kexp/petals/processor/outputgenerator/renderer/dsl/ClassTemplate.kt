package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

sealed class BaseTypeTemplate<TYPE> constructor(
    private val modifiers: Collection<KModifier>?,
    annotations: Collection<AnnotationTemplate>?,
    function: TYPE.() -> Unit
): KotlinContainerTemplate {

    abstract val typeBuilder: TypeSpec.Builder

    internal val typeSpec: TypeSpec by lazy {
        annotations?.let {
            typeBuilder.addAnnotations(
                it.map { annotationTemplate -> annotationTemplate.annotationSpec }
            )
        }
        modifiers?.let { typeBuilder.addModifiers(modifiers) }

        // This is a sealed class, and we can see that this is true for all child classes
        @Suppress("UNCHECKED_CAST")
        (this as TYPE).function()

        return@lazy typeBuilder.build()
    }

    override fun addFunction(functionTemplate: FunctionTemplate) {
        typeBuilder.addFunction(functionTemplate.functionSpec)
    }

    override fun addProperties(properties: Collection<PropertyTemplate>) {
        typeBuilder.addProperties(properties.map { it.propertySpec })
    }

    fun addPrimaryConstructor(primaryConstructorTemplate: ConstructorTemplate) {
        typeBuilder.primaryConstructor(primaryConstructorTemplate.constructorSpec)
    }

    fun addSuperclass(superclassTemplate: SuperclassTemplate) {
        typeBuilder.superclass(superclassTemplate.className)
        superclassTemplate.constructorParams.forEach { constructorParam ->
            typeBuilder.addSuperclassConstructorParameter(constructorParam.codeBlock)
        }
    }
}


open class ClassTemplate protected constructor(
    className: ClassName,
    modifiers: Collection<KModifier>?,
    annotations: Collection<AnnotationTemplate>?,
    function: ClassTemplate.() -> Unit
): BaseTypeTemplate<ClassTemplate>(
    modifiers = modifiers,
    annotations = annotations,
    function = function
)
{
    override val typeBuilder = TypeSpec.classBuilder(className)

    internal fun addCompanionObject(companionObjectTemplate: CompanionObjectTemplate) {
        typeBuilder.addType(companionObjectTemplate.typeSpec)
    }

    fun performOnTypeBuilder(function: TypeSpec.Builder.() -> Unit) {
        typeBuilder.function()
    }

    companion object {
        fun FileTemplate.classTemplate(
            className: ClassName,
            modifiers: Collection<KModifier>? = null,
            annotations: Collection<AnnotationTemplate>? = null,
            function: ClassTemplate.() -> Unit,
        ) {
            addClass(ClassTemplate(className, modifiers, annotations, function))
        }
    }
}

class ObjectTemplate private constructor(
    className: ClassName,
    modifiers: Collection<KModifier>?,
    annotations: Collection<AnnotationTemplate>?,
    function: ClassTemplate.() -> Unit
): ClassTemplate(
    className = className,
    modifiers = modifiers,
    annotations = annotations,
    function = function
) {

    override val typeBuilder = TypeSpec.objectBuilder(className)

    companion object {
        fun FileTemplate.objectTemplate(
            className: ClassName,
            modifiers: Collection<KModifier>? = null,
            annotations: Collection<AnnotationTemplate>? = null,
            function: ClassTemplate.() -> Unit,
        ) {
            addObject(ObjectTemplate(className, modifiers, annotations, function))
        }
    }
}

class CompanionObjectTemplate private constructor(
    modifiers: Collection<KModifier>?,
    annotations: Collection<AnnotationTemplate>?,
    function: CompanionObjectTemplate.() -> Unit
): BaseTypeTemplate<CompanionObjectTemplate>(
    modifiers = modifiers,
    annotations = annotations,
    function = function
) {

    override val typeBuilder = TypeSpec.companionObjectBuilder()

    companion object {
        fun ClassTemplate.companionObjectTemplate(
            modifiers: Collection<KModifier>? = null,
            annotations: Collection<AnnotationTemplate>? = null,
            function: CompanionObjectTemplate.() -> Unit,
        ) {
            addCompanionObject(CompanionObjectTemplate(modifiers, annotations, function))
        }
    }
}
