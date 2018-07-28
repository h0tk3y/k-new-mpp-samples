package com.example.app

import kotlin.io.*

actual class Greeter : Greeting {
	override fun showGreeting(user: User) { }
}

actual fun getCurrentUser(): User {
	println("What's your name?")
	return User("anonymous")
}

fun main(args: Array<String>): Unit = runGreeter()