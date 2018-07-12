package com.example.app

import com.example.lib.arithmetic.Arithmetic
import com.example.lib.strings.Strings

object Main {
	@JvmStatic
	fun main(args: Array<String>) {
		val (x, y, z) = (1..3).map { readLine()!!.toInt() }
		val (r1, r2, r3) = listOf(
			Arithmetic.multiplyAdd(x, y, z), 
			Arithmetic.multiplyAdd(y, x, z),
			Arithmetic.multiplyAdd(z, x, y)
		).map { "$it" }
		println(Strings.concatAll(r1, r2, r3))
	}
}