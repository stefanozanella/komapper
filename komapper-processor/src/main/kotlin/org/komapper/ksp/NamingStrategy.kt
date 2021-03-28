package org.komapper.ksp

import java.nio.CharBuffer

interface NamingStrategy {

    fun apply(name: String): String
}

object CamelToLowerSnakeCase : NamingStrategy {

    private val camelToSnakeCase = CamelToSnakeCase(Char::toLowerCase)

    override fun apply(name: String): String {
        return camelToSnakeCase.apply(name)
    }
}

object CamelToUpperSnakeCase : NamingStrategy {

    private val camelToSnakeCase = CamelToSnakeCase(Char::toUpperCase)

    override fun apply(name: String): String {
        return camelToSnakeCase.apply(name)
    }
}

private class CamelToSnakeCase(private val mapper: (Char) -> Char) {

    fun apply(name: String): String {
        val builder = StringBuilder()
        val buf = CharBuffer.wrap(name)
        while (buf.hasRemaining()) {
            val c1 = buf.get()
            builder.append(mapper(c1))
            buf.mark()
            if (buf.hasRemaining()) {
                val c2 = buf.get()
                if (!c1.isUpperCase() && c2.isUpperCase()) {
                    builder.append("_")
                }
                buf.reset()
            }
        }
        return builder.toString()
    }
}

object Implicit : NamingStrategy {
    override fun apply(name: String): String {
        return name
    }
}