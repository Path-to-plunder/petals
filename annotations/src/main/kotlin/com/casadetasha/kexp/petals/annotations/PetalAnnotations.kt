package com.casadetasha.kexp.petals.annotations

import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Petal(val tableName: String,
                             val className: String,
                             val primaryKeyType: PetalPrimaryKey = PetalPrimaryKey.UUID)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PetalSchema(val petal: KClass<out Any>, val version: Int = 1)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class ExecuteSqlBeforeMigration(val executableSql: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class IncludeExpiration(val duration: Long, val durationUnit: PetalDuration)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class IncludeTimestamps

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DefaultString(val value: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DefaultInt(val value: Int)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DefaultLong(val value: Long)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class AlterColumn(val renameFrom: String = "")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class VarChar(val charLimit: Int)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DoNotExport

annotation class ReferencedBy(val referencePropertyName: String)

enum class PetalPrimaryKey(val dataType: String?) {
    INT("SERIAL"),
    LONG("BIGSERIAL"),
    UUID("uuid")
}

enum class PetalDuration {
    DAYS,
    HOURS,
    MINUTES,
    SECONDS,
    MILLISECONDS
}

fun Long.toMillis(petalDuration: PetalDuration): Long {
    return when (petalDuration) {
        PetalDuration.DAYS -> days.inWholeMilliseconds
        PetalDuration.HOURS -> hours.inWholeMilliseconds
        PetalDuration.MINUTES -> minutes.inWholeMilliseconds
        PetalDuration.SECONDS -> seconds.inWholeMilliseconds
        PetalDuration.MILLISECONDS -> this
    }
}
