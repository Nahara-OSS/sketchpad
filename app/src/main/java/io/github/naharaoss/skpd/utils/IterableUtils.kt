package io.github.naharaoss.skpd.utils

fun <T> Iterable<T>.split(predicate: (T) -> Boolean): Pair<List<T>, List<T>> {
    val a = mutableListOf<T>()
    val b = mutableListOf<T>()

    for (item in this) {
        (if (predicate(item)) a else b).add(item)
    }

    return a to b
}

fun <T> Iterable<T>.prepend(item: T): List<T> {
    val list = mutableListOf<T>()
    list.add(item)
    list.addAll(this)
    return list
}