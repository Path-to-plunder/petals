package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.processor.model.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.ParsedPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.ReferencedByPetalColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.toMemberName
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate
import com.casadetasha.kexp.generationdsl.dsl.PropertyTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedEntityTemplateValues.getEntityClassSimpleName
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedEntityTemplateValues.getTableClassSimpleName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import java.util.*

internal object ExposedEntityTemplateValues {
    fun ParsedPetalSchema.getTableClassSimpleName(): String = "${baseClassName}Table"
    fun ParsedPetalSchema.getEntityClassSimpleName(): String = "${baseClassName}Entity"
}

internal fun FileTemplate.createExposedEntityClassTemplate(
    packageName: String,
    petalClasses: PetalClasses,
    schema: ParsedPetalSchema
) =
    generateClass(className = ClassName(packageName, schema.getEntityClassSimpleName())) {
        generatePrimaryConstructor {
            generateParameterTemplate(
                name = "id",
                typeName = EntityID::class.asClassName().parameterizedBy(schema.getEntityIdParameter())
            )
        }

        generateSuperClass(schema.getSchemaEntitySuperclass()) {
            generateConstructorParam { CodeTemplate("id") }
        }

        collectPropertyTemplates {
            schema.parsedPetalColumns
                .filterNot { it is PetalIdColumn }
                .map { it.toEntityColumn(petalClasses) }
        }

        generateCompanionObject {
            generateSuperClass(className = schema.getEntityCompanionSuperclassName(packageName)) {
                generateConstructorParam { CodeTemplate(schema.getTableClassSimpleName()) }
            }
        }
    }

private fun ParsedPetalColumn.toEntityColumn(petalClasses: PetalClasses): PropertyTemplate {
    return when (this) {
        is PetalReferenceColumn -> toReferenceEntityColumn()
        is ReferencedByPetalColumn -> toReferencedByColumn(petalClasses)
        is LocalPetalColumn -> toNonReferenceLocalColumn()
    }
}

private fun PetalReferenceColumn.toReferenceEntityColumn(): PropertyTemplate {
    val referencedOnMethod: String = if (isNullable) { "optionalReferencedOn" } else { "referencedOn" }
    return PropertyTemplate(
        name = name,
        typeName = referencingEntityClassName.copy(nullable = isNullable),
        isMutable = true
    ) {
        delegate {
            CodeTemplate(
                "%M·%L·%L.%L",
                referencingEntityClassName.toMemberName(),
                referencedOnMethod,
                parentSchema.getTableClassSimpleName(),
                name,
            )
        }
    }
}

private fun ReferencedByPetalColumn.toReferencedByColumn(petalClasses: PetalClasses): PropertyTemplate {
    val referencedByColumnInfo = referencedByColumn.columnReference
    val referencedByColumnType = referencedByColumn.columnReference.kotlinTypeName
    val externalReferenceColumn = petalClasses.schemaMap[referencedByColumnType]
        ?: printThenThrowError("INTERNAL LIBRARY ERROR: Petal type $referencedByColumnType not found" +
                " when creating load references method for column $name for petal ${parentSchema.baseClassName}. This should" +
                " have been caught during initial petal parsing")
    val referencedByLocalColumn = externalReferenceColumn.parsedLocalPetalColumnMap[referencedByColumn.columnName]
        ?: printThenThrowError("ReferencedBy column with name ${referencedByColumn.columnName}" +
                " not found for petal type $referencedByColumnType when constructing load references method for" +
                " column ${name} for petal ${parentSchema.baseClassName}")

    val referrersOnMethod: String = if (referencedByLocalColumn.isNullable) { "optionalReferrersOn" } else { "referrersOn" }
    return PropertyTemplate(
        name = name,
        typeName = SizedIterable::class.asClassName().parameterizedBy(referencedByColumnInfo.entityClassName)
    ) {
        delegate {
            CodeTemplate(
                "%M·%L·%M.%L",
                referencedByColumnInfo.entityClassName.toMemberName(),
                referrersOnMethod,
                referencedByColumnInfo.tableClassName.toMemberName(),
                referencedByColumn.columnName,
            )
        }
    }
}

private fun LocalPetalColumn.toNonReferenceLocalColumn(): PropertyTemplate {
    return PropertyTemplate(
        name = name,
        typeName = kotlinType.copy(nullable = isNullable),
        isMutable = true,
    ) {
        delegate {
            CodeTemplate("%L.%L", parentSchema.getTableClassSimpleName(), name)
        }
    }
}

private fun ParsedPetalSchema.getEntityCompanionSuperclassName(packageName: String): ParameterizedTypeName {
    return when (primaryKeyType) {
        PetalPrimaryKey.INT -> IntEntityClass::class.asClassName()
        PetalPrimaryKey.LONG -> LongEntityClass::class.asClassName()
        PetalPrimaryKey.UUID -> UUIDEntityClass::class.asClassName()
    }.parameterizedBy(ClassName(packageName, getEntityClassSimpleName()))
}

private fun ParsedPetalSchema.getSchemaEntitySuperclass(): ClassName {
    return when (primaryKeyType) {
        PetalPrimaryKey.INT -> IntEntity::class.asClassName()
        PetalPrimaryKey.LONG -> LongEntity::class.asClassName()
        PetalPrimaryKey.UUID -> UUIDEntity::class.asClassName()
    }
}

private fun ParsedPetalSchema.getEntityIdParameter(): ClassName {
    return when (primaryKeyType) {
        PetalPrimaryKey.INT -> Int::class.asClassName()
        PetalPrimaryKey.LONG -> Long::class.asClassName()
        PetalPrimaryKey.UUID -> UUID::class.asClassName()
    }
}
