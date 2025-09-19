package dev.samicpp.web

import dev.samicpp.http.example
import dev.samicpp.http.TcpSocket
import dev.samicpp.http.Http1Socket
import java.net.ServerSocket
import java.net.InetAddress
import java.lang.System
import kotlin.io.path.readText
import kotlinx.serialization.json.*
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files
// val esc="\u001b"

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
            hand.readClient()
            hand.close("hello from ${hand.client.path}\n")
            hand.client.apply { 
                println("\u001b[32mclient { \n   path: $path, \n   method: $method, \n   version: $version, \n   headers: $headers, \n   body[${body.size}]: \"${body.decodeToString()}\", \n}\u001b[0m")
            }   
        }
    }
}

fun server(){
    val listener=ServerSocket(port,50,InetAddress.getByName(host))
    println("listening")
    while (true){
        val conn=listener.accept()
        println("\u001b[32maccepted connection from ${conn.remoteSocketAddress.toString()}\u001b[0m")
        Thread.startVirtualThread{
            val hand=Http1Socket(TcpSocket(conn))
            hand.readClient()
            // hand.close("hello from ${hand.client.path}\n")
            handler(hand)
        }
    }
}

var serve_dir="./public"
var port=3000
var host="0.0.0.0"
var useContextPool=true

// val shared=ConcurrentHashMap<String,Any?>()
//mutableMapOf<String,Any?>()

fun main(){
    println("\u001b[35mweb service working directory\u001b[0m ${System.getProperty("user.dir")}")
    // echo_test()
    // http_dump_test()
    serve_dir=System.getenv("SERVE_DIR")?:serve_dir
    host=System.getenv("HOST_ADDRESS")?:host
    port=System.getenv("PORT")?.toInt()?:port

    val jconf=Paths.get("./settings.json")
    if(Files.exists(jconf)){
        val map: Map<String, String> = Json.decodeFromString(jconf.readText())
        
        if(map["serve_dir"]!=null)serve_dir=map["serve_dir"]!!
        if(map["host"]!=null)host=map["host"]!!
        if(map["port"]!=null)port=map["port"]!!.toInt()
        if(map["useContextPool"]!=null)useContextPool=map["useContextPool"]!!.toBoolean()
    }
    
    println("\u001b[32mserve dir = $serve_dir\naddress = $host:$port\nworking dir = ${System.getProperty("user.dir")}\u001b[0m")

    // val supported=polyCtx.getEngine().getLanguages()
    // println("supported poly langs $supported")

    // val shared=object{}
    // polyCtx.getBindings("js").putMember("shared",shared)
    // polyCtx.getBindings("python").putMember("shared",shared)

    // poltCtx["shared"]=shared

    setup()

    HpackRoundTripTest.main()

    server()
}
