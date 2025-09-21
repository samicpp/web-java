package dev.samicpp.web

import dev.samicpp.http.example
import dev.samicpp.http.TcpSocket
import dev.samicpp.http.Http1Socket
import dev.samicpp.http.Http2Connection
import dev.samicpp.http.Http2Settings
import java.net.ServerSocket


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
            hand.readClient()
            hand.close("hello from ${hand.client.path}\n")
            hand.client.apply { 
                println("\u001b[32mclient { \n   path: $path, \n   method: $method, \n   version: $version, \n   headers: $headers, \n   body[${body.size}]: \"${body.decodeToString()}\", \n}\u001b[0m")
            }   
        }
    }
}

fun http2frame_dump_test(){
    val server=ServerSocket(3000)
    println("listening")
    while (true){
        val conn=server.accept()
        println("accepted connection from ${conn.remoteSocketAddress.toString()}")
        Thread.startVirtualThread{
            val hand=Http2Connection(TcpSocket(conn))
            hand.sendSettings(Http2Settings(max_frame_size=65535))
            val frames=hand.incoming()
            hand.handle(frames)
            println(frames)
            hand.sendHeaders(1, listOf(":status" to "200","content-type" to "text/plain"), false)
            hand.sendData(1, "payload".encodeToByteArray(), true)
        }
    }
}

fun http2_upgrade_test(){
    val server=ServerSocket(3000)
    println("listening")
    while (true){
        val conn=server.accept()
        println("accepted connection from ${conn.remoteSocketAddress.toString()}")
        Thread.startVirtualThread{
            val hand=Http1Socket(TcpSocket(conn))
            val client=hand.readClient()
            val upgrade=client.headers["upgrade"]?.get(0)
            println("upgrade = $upgrade")
            if(upgrade=="h2c"){
                val h2=hand.h2c()
                println("succesfully upgraded")
                h2.sendSettings(Http2Settings(max_frame_size=65535))
                val frames=h2.incoming()
                h2.handle(frames)
                h2.sendHeaders(1, listOf(":status" to "200","content-type" to "text/plain"), false)
                h2.sendData(1, "payload".encodeToByteArray(), true)
            } else {
                hand.close("http2 when?")
            }
        }
    }
}