package com.example.lib.strings

object Strings {
	fun concatAll(vararg strings: String) = strings.toList().joinToString("")
}