package com.example.app

import kotlin.io.*
import java.io.BufferedReader

actual class Greeter : Greeting {
	override fun showGreeting(user: User) {
		System.`out`.printf(greet(user))
	}
}

actual fun getCurrentUser(): User {
	println("What's your name?")
	return User(readLine() ?: "anonymous")
}

fun main(args: Array<String>): Unit = runGreeter()