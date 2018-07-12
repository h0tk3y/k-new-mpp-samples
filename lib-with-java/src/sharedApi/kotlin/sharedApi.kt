package com.example.api

interface MyApi {
	val id: String
    fun myApi(myApi: MyApi): MyApi = myApi
}