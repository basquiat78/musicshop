package io.basquiat.musicshop.common.extensions

fun <T, R> Iterable<T>.countZipWith(other: R): Pair<Iterable<T>, R> {
    return this to other
}

fun <T, R, S> Pair<Iterable<T>, R>.map(transformer: (Pair<Iterable<T>, R>) -> S): S {
    return transformer(this)
}