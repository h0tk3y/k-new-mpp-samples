package com.example.jvm

import com.example.api

class MyApiImpl : MyApi {
	override val id: String get() = "jvmImpl"
	override fun myApi(myApi: MyApi): MyApi = MyApiImpl()
}