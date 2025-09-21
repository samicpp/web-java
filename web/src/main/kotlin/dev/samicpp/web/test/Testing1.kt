package dev.samicpp.web

import dev.samicpp.http.example
import dev.samicpp.http.TcpSocket
import dev.samicpp.http.TlsSocket
import dev.samicpp.http.Http1Socket
import dev.samicpp.http.Http2Connection
import dev.samicpp.http.Http2Settings
import dev.samicpp.http.Http2Stream
import java.net.ServerSocket
import javax.net.ssl.*
import java.security.KeyStore


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

fun tls_serve_test(){
    val keyStore=KeyStore.getInstance("PKCS12")
    keyStore.load(java.io.FileInputStream("../ssl/localhost.p12"), "".toCharArray())

    val kmf=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(keyStore, "".toCharArray())

    val sslContext=SSLContext.getInstance("TLS")
    sslContext.init(kmf.keyManagers, null, null)

    val serverSocket=sslContext.serverSocketFactory.createServerSocket(4000) as SSLServerSocket

    serverSocket.sslParameters = serverSocket.sslParameters.apply {
        applicationProtocols = arrayOf("h2","http/1.1")
    }
    serverSocket.enabledProtocols=arrayOf("TLSv1.2","TLSv1.3")

    println("ssl listening")

    server@
    while (true) {
        val conn=serverSocket.accept() as SSLSocket
        try{
            conn.startHandshake()
        }catch(err:Throwable){
            println("\u001b[31mhandshake error occured\u001b[0m")
            println(err)
            continue@server
        }
        val alpn=conn.applicationProtocol
        println("\u001b[32maccepted connection from ${conn.remoteSocketAddress.toString()}\nalpn = $alpn\u001b[0m")

        Thread.startVirtualThread{
            if(alpn=="h2"){
                val h2=Http2Connection(TlsSocket(conn))
                var frames=h2.incoming()
                h2.sendSettings(Http2Settings())
                while(true){
                    try {
                        println("${frames.size} incoming frames")
                        if(frames.size==0){
                            println("client disconnected")
                            break
                        }
                        for(s in h2.handle(frames)){
                            println("new stream")
                            h2.sendHeaders(s, listOf(":status" to "200","content-type" to "text/plain"))
                            h2.sendData(s, "payload".encodeToByteArray(), true)
                        }
                        frames=h2.incoming()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        h2.flush()
                    }
                }
            } else /*if(alpn=="http/1")*/ {
                val hand=Http1Socket(TlsSocket(conn))
                hand.status=501
                hand.statusMessage="Not Implemented"
                hand.close("connect with http2")
            }
        }
    }
}