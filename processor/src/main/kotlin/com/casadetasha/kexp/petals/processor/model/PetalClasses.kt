package com.casadetasha.kexp.petals.processor.model

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.*
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetal
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetalSchema
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import kotlin.reflect.KClass

internal class PetalClasses {

    val PETAL_CLASSES: Map<ClassName, KotlinContainer.KotlinClass> by lazy {
        AnnotationParser.getClassesAnnotatedWith(
            annotationClass = Petal::class
        ).associateBy { it.className }
    }

    val SCHEMA_CLASSES: Set<KotlinContainer.KotlinClass> by lazy {
        AnnotationParser.getClassesAnnotatedWith(
            annotationClass = PetalSchema::class,
            propertyAnnotations = SUPPORTED_PROPERTY_ANNOTATIONS
        )
    }

    val SUPPORTED_PROPERTY_ANNOTATIONS =
        listOf(
            AlterColumn::class,
            VarChar::class,
            DefaultInt::class,
            DefaultString::class,
            DefaultLong::class,
            ReferencedBy::class,
        )

    companion object {
        internal val SUPPORTED_TYPES = listOf<KClass<*>>(
            String::class,
            Int::class,
            Long::class,
            UUID::class
        ).map { it.asTypeName() }
    }

    private var _RUNTIME_SCHEMAS: Map<TypeName, UnprocessedPetalSchemaMigration>? = null
    var RUNTIME_SCHEMAS: Map<TypeName, UnprocessedPetalSchemaMigration>
    get() {
        return checkNotNull(_RUNTIME_SCHEMAS) { "INTERNAL LIBRARY ERROR: Runtime schemas must be set before accessing" }
    }
    set(value) {
//        check(_RUNTIME_SCHEMAS == null) { "INTERNAL LIBRARY ERROR: Runtime schemas must only be set once"}
        _RUNTIME_SCHEMAS = value
    }

    lateinit var petalToSchemaMap: Map<ClassName, ParsedPetalSchema>
}