package com.casadetasha.kexp.petals.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Petal(val tableName: String,
                       val version: Int = 1,
                       val primaryKeyType: PetalPrimaryKey = PetalPrimaryKey.NONE)

enum class PetalPrimaryKey {
    NONE,
    INT_AUTO_INCREMENT,
    INT,
    TEXT,
    UUID
}

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class AlterColumn(val previousName: String = "")
