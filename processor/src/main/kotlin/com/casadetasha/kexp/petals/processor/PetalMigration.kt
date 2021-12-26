package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer.KotlinClass
import com.casadetasha.kexp.annotationparser.KotlinValue.KotlinProperty
import com.casadetasha.kexp.petals.annotations.Petal
import com.squareup.kotlinpoet.TypeName

data class PetalMigration(val tableName: String, val version: Int, val columns: Set<PetalColumn>) {

    companion object {
        fun parseFromClass(kotlinClass: KotlinClass): PetalMigration {
            val petalAnnotation = kotlinClass.getAnnotation(Petal::class)
                ?: printThenThrowError(
                    "Cannot parse petal migration from class ${kotlinClass.className}:" +
                            " petal class must contain petal annotation ")

            return PetalMigration(
                tableName = petalAnnotation.tableName,
                version = petalAnnotation.version,
                columns = parsePetalColumns(kotlinClass.kotlinProperties)
            )
        }

        private fun parsePetalColumns(kotlinProperties: List<KotlinProperty>): Set<PetalColumn> {
            val columnSet = HashSet<PetalColumn>()
            kotlinProperties.forEach {
                columnSet.add(PetalColumn.parseFromProperty(it))
            }

            return columnSet
        }
    }
}

data class PetalColumn(val name: String, val typeName: TypeName) {

    companion object {
        fun parseFromProperty(kotlinProperty: KotlinProperty): PetalColumn {
            return PetalColumn(
                name = kotlinProperty.simpleName,
                typeName = kotlinProperty.typeName
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PetalColumn) return false

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
