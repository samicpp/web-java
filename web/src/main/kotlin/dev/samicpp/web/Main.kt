package dev.samicpp.web

import dev.samicpp.http.example

fun main() {
    println("main func called")
    example()
    var some=0
    while(true){
        println("loop $some")
        some+=1
        Thread.sleep(2000)
    }
}
