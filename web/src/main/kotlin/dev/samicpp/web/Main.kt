package dev.samicpp.web

import dev.samicpp.http.example
import dev.samicpp.http.TcpSocket
import dev.samicpp.http.Http1Socket
import java.net.ServerSocket

val esc="\u001b"

fun old_main() {
    println("main func called")
    example()
    var some=0
    while(true){
        println("loop $some")
        some+=1
        Thread.sleep(2000)
    }
}

fun echo_test(){
    val server=ServerSocket(3000)
    println("listening")
    while (true){
        val conn=server.accept()
        println("accepted connection from ${conn.remoteSocketAddress.toString()}")
        Thread.startVirtualThread{
            val sock=TcpSocket(conn)
            var buff=ByteArray(1024)
            while (true){
                val read=sock.read(buff)
                val slice=buff.sliceArray(0..<read)
                val str=slice.toString(Charsets.UTF_8) //Charsets.UTF_8
                println("client said [$read]: \u001b[36m$str\u001b[0m")
                if (read==-1){
                    println("client likely disconnected")
                    break
                }
            }
        }
    }
}

fun http_dump_test(){
    val server=ServerSocket(3000)
    println("listening")
    while (true){
        val conn=server.accept()
        println("accepted connection from ${conn.remoteSocketAddress.toString()}")
        Thread.startVirtualThread{
            val hand=Http1Socket(TcpSocket(conn))
            hand.read_client()
            hand.close("hello from ${hand.client.path}\n")
            hand.client.apply { 
                println("\u001b[32mclient { \n   path: $path, \n   method: $method, \n   version: $version, \n   headers: $headers, \n   body[${body.size}]: \"${body.decodeToString()}\", \n}\u001b[0m")
            }   
        }
    }
}

fun main(){
    // echo_test()
    http_dump_test()
}
