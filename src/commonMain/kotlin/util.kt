package kask

private val base62 = ((48..57) + (65..90) + (97..122)).map { it.toChar() }
fun randomString(len: Int): String = (1..len).map { base62.random() }.joinToString("")
