package dev.samicpp.web

import dev.samicpp.http.example
import dev.samicpp.http.TcpSocket
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

fun main(){
    val server=ServerSocket(3456)
    println("listening")
    while (true){
        val conn=server.accept()
        println("accepted message from ${conn.remoteSocketAddress.toString()}")
        Thread.startVirtualThread{
            val sock=TcpSocket(conn)
            var buff=ByteArray(1024)
            while (true){
                val read=sock.read(buff)
                val slice=buff.sliceArray(0..<read)
                val str=slice.toString(Charsets.UTF_8) //Charsets.UTF_8
                println("client said: \u001b[36m$str\u001b[0m")
            }
        }
    }
}
