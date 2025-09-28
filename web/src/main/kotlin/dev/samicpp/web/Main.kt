package dev.samicpp.web

import dev.samicpp.http.example
import dev.samicpp.http.TcpSocket
import dev.samicpp.http.TlsSocket
import dev.samicpp.http.Http1Socket
import dev.samicpp.http.Http2Connection
import dev.samicpp.http.Http2Settings
import dev.samicpp.http.StreamData

import java.net.ServerSocket
import java.net.InetAddress
import java.lang.System
import kotlin.io.path.readText
import kotlinx.serialization.json.*
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files
import javax.net.ssl.*
import java.security.KeyStore
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

var serverSettings=Http2Settings(
    header_table_size=16777215,
    initial_window_size=65535,
    max_frame_size=65535,
    max_concurrent_streams=1073741824,
)

fun server(port:Int,host:String){
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

fun sslServer(sslPath:String,sslPassword:String,port:Int,host:String){
    val keyStore=KeyStore.getInstance("PKCS12")
    keyStore.load(java.io.FileInputStream(sslPath), sslPassword.toCharArray())

    val kmf=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    kmf.init(keyStore, sslPassword.toCharArray())

    val sslContext=SSLContext.getInstance("TLS")
    sslContext.init(kmf.keyManagers, null, null)

    val serverSocket=sslContext.serverSocketFactory.createServerSocket(port,50,InetAddress.getByName(host)) as SSLServerSocket

    serverSocket.sslParameters = serverSocket.sslParameters.apply {
        applicationProtocols = alpn.split(";").toTypedArray()
    }
    serverSocket.enabledProtocols=arrayOf("TLSv1.2","TLSv1.3")

    println("alpn order is $alpn")

    println("ssl listening")

    server@
    while (true) {
        val conn=try{
            serverSocket.accept() as SSLSocket
        } catch(err:Exception){
            println("\u001b[31mtls error occured\u001b[0m")
            err.printStackTrace()
            continue@server
        }
        Thread.startVirtualThread{
            try{
                conn.startHandshake()
            }catch(err:Exception){
                println("\u001b[31mhandshake error occured\u001b[0m")
                println(err)
                try { conn.close() } catch (_: Exception) {}
                // err.printStackTrace()
                return@startVirtualThread
            } catch(err:Throwable){
                println("\u001b[31munkown handshake error occured\u001b[0m")
                println(err)
                try { conn.close() } catch (_: Exception) {}
                return@startVirtualThread
            }
            val alpn=conn.applicationProtocol
            println("\u001b[32maccepted connection from ${conn.remoteSocketAddress.toString()}\nalpn = $alpn\u001b[0m")

            if(alpn=="h2"){
                val h2=Http2Connection(TlsSocket(conn))
                h2.hpackd.updateDynamicTableSize(16777215)
                h2.sendSettings(serverSettings); h2.flush()
                println("sent settings $serverSettings")
                h2.sendPing("hearbeat".encodeToByteArray())
                // var empties=0
                // print("buffer = [ ")
                // for(b in serverSettings.toBuffer())print("${b.toInt() and 0xff}, ")
                // println("]")
                loop@
                while(true){
                    // if(!h2.available())continue@loop
                    try{
                        val frames=h2.incoming()
                        if(frames.isEmpty()){
                            println("\u001b[31mclient disconnected\u001b[0m")
                            // empties+=1
                            // if(h2.closed||empties>10)break@loop
                            // h2.close()
                            break@loop
                        }
                        println("received ${frames.size} frames [")
                        for(frame in frames)println("   \u001b[34m${frame.type}\u001b[0m,")
                        println("]")

                        for(frame in frames){
                            when(frame.stringType){
                                "Settings"->println("client sent settings ${frame.settings}")
                            }
                        }

                        val opened=h2.handle(frames)
                        println("opened ${opened.size} streams")
                        for(s in opened){
                            val stream=h2.getStream(s)
                            stream.readClient()
                            Thread.startVirtualThread{ handler(stream) }
                        }
                        // h2.streamData[1]=StreamData(true,false,123456,listOf(":path" to "/", ":authority" to "localhost"))
                        // h2.sendSettingsAck()
                        // h2.sendHeaders(1, listOf(":status" to "500"))
                        // h2.sendData(1,"client did NOT send headers".encodeToByteArray(),true)
                        // h2.sendPing("hearbeat".encodeToByteArray())
                    } catch(err:Exception){
                        println("\u001b[31merror occured\u001b[0m")
                        err.printStackTrace()
                    } catch(err:Throwable){
                        println("\u001b[31munkown error occured\u001b[0m")
                        println(err)
                    }
                }
            } else /*if(alpn=="http/1")*/ {
                val hand=Http1Socket(TlsSocket(conn))
                hand.readClient()
                handler(hand)
            }
        }
    }
}

var serve_dir="./public"
var port=3000
var host="0.0.0.0"
var useContextPool=true
var pkcsCert:String?=null
var alpn="http/1.1;h2"

// val shared=ConcurrentHashMap<String,Any?>()
//mutableMapOf<String,Any?>()

fun main(){
    println("\u001b[35mweb service working directory\u001b[0m ${System.getProperty("user.dir")}")
    
    serve_dir=System.getenv("SERVE_DIR")?:serve_dir
    host=System.getenv("HOST_ADDRESS")?:host
    port=System.getenv("PORT")?.toInt()?:port
    pkcsCert=System.getenv("SSL_PATH")?:pkcsCert
    var pkcsPass=System.getenv("SSL_PASSWORD")?:""
    alpn=System.getenv("ALPN_ORDER")?:alpn

    val jconf=Paths.get("./settings.json")
    if(Files.exists(jconf)){
        val map: Map<String, String> = Json.decodeFromString(jconf.readText())
        
        if(map["serve_dir"]!=null)serve_dir=map["serve_dir"]!!
        if(map["host"]!=null)host=map["host"]!!
        if(map["port"]!=null)port=map["port"]!!.toInt()
        if(map["alpn"]!=null)alpn=map["alpn"]!!
        if(map["sslPath"]!=null)pkcsCert=map["sslPath"]!!
        if(map["sslPassword"]!=null)pkcsPass=map["sslPassword"]!!
        if(map["useContextPool"]!=null)useContextPool=map["useContextPool"]!!.toBoolean()
    }
    
    println("\u001b[32mserve dir = $serve_dir\naddress = $host:$port\nworking dir = ${System.getProperty("user.dir")}\nuses tls = ${pkcsCert!=null}\u001b[0m")

    Debug.start()

    setup()
    if(pkcsCert!=null)sslServer(pkcsCert!!,pkcsPass,port,host)
    else server(port,host)
    // HpackRoundTripTest.main()
    // http2frame_dump_test()
    // http2_upgrade_test()
    // tls_serve_test()
}
