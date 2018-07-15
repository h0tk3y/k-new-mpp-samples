package com.example.app

actual fun f() { }

object Main {
	@JvmStatic
	fun main(args: Array<String>) {
		f()
	}
}