package com.example.app

import com.example.hello

class User(val name: String)

expect fun getCurrentUser(): User

interface Greeting {
	fun greet(user: User): String = hello(user.name)
	fun showGreeting(user: User): Unit
}

expect class Greeter

fun runGreeter(): Unit {
	Greeter().showGreeting(getCurrentUser())
} 