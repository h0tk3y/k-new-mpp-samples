package com.example.nodeJs

import com.example.api.*

class MyApiImpl : MyApi {
	override val id: String get() = "nodeJsImpl"
	override fun myApi(myApi: MyApi): MyApi = MyApiImpl()
}