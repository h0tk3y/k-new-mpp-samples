package com.example

expect fun hello(name: String): String

fun bye(name: String) = hello(name).reversed()