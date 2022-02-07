package kask

typealias PropertySet = Map<PropertyKey<*>, Property<*>>

object EmptyPropertySet : PropertySet by emptyMap()

inline fun <reified T : Any> PropertySet.prop(key: PropertyKey<T>): T? =
    when (val value = get(key)?.value) {
        is T -> value
        null -> null
        else -> error("Impossible")
    }

data class Property<T : Any>(val value: T)

open class PropertyKey<in T : Any>
