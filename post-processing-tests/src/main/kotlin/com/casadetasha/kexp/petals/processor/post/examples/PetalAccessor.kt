package com.casadetasha.kexp.petals.processor.post.examples

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.Entity

@Serializable
abstract class PetalAccessor<T: Entity<*>, S>(@Transient var entity: T? = null) {

}
