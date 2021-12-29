package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer.KotlinClass
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.AlterColumn
import com.casadetasha.kexp.petals.annotations.Petal
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass

data class PetalMigration(val tableName: String,
                          val version: Int,
                          val columnMigrations: HashMap<String, PetalMigrationColumn>) {

    companion object {
        fun parseFromClass(kotlinClass: KotlinClass): PetalMigration {
            val petalAnnotation = kotlinClass.getAnnotation(Petal::class)
                ?: printThenThrowError(
                    "Cannot parse petal migration from class ${kotlinClass.className}:" +
                            " petal class must contain petal annotation ")

            return PetalMigration(
                tableName = petalAnnotation.tableName,
                version = petalAnnotation.version,
                columnMigrations = parsePetalColumns(kotlinClass.kotlinProperties)
            )
        }

        private fun parsePetalColumns(kotlinProperties: List<KotlinProperty>): HashMap<String, PetalMigrationColumn> {
            val columnMap = HashMap<String, PetalMigrationColumn>()
            kotlinProperties.forEach {
                columnMap[it.simpleName] = PetalMigrationColumn.parseFromProperty(it)
            }

            return columnMap
        }
    }
}

data class PetalMigrationColumn(val name: String,
                                val typeName: TypeName,
                                val kotlinProperty: KotlinProperty) {

    companion object {
        private val SUPPORTED_TYPES = listOf<KClass<*>>(
            String::class,
            Int::class,
            Long::class,
            UUID::class
        ).map { it.asTypeName() }

        fun parseFromProperty(kotlinProperty: KotlinProperty): PetalMigrationColumn {
            return PetalMigrationColumn(
                name = kotlinProperty.simpleName,
                typeName = kotlinProperty.typeName,
                kotlinProperty = kotlinProperty
            ).apply {
                checkTypeValidity(typeName)
            }
        }

        private fun checkTypeValidity(typeName: TypeName) {
            if (!SUPPORTED_TYPES.contains(typeName.copy(nullable = false))) {
                printThenThrowError(
                    "$typeName is not a valid column type. Only the following types are supported:" +
                            " ${SUPPORTED_TYPES.joinToString()}")
            }
        }
    }

    val isNullable: Boolean by lazy { kotlinProperty.isNullable }

    private val alterColumnAnnotation: AlterColumn? by lazy {
        kotlinProperty.annotatedElement?.getAnnotation(AlterColumn::class.java)
    }

    val isAlteration: Boolean by lazy { alterColumnAnnotation != null }

    val previousName: String by lazy {
        if (!isAlteration) printThenThrowError("Only AlterColumn annotated properties can have previousName")
        return@lazy alterColumnAnnotation!!.previousName
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PetalMigrationColumn) return false

        if (name != other.name) return false
        if (typeName != other.typeName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + typeName.hashCode()
        return result
    }
}
