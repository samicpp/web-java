package dev.samicpp.web


import dev.samicpp.http.HttpSocket
import dev.samicpp.http.FakeHttpSocket

// import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpRequest.BodyPublishers
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.file.Paths
import java.time.Instant
import java.time.Duration
import kotlin.collections.listOf


fun setup(warmup:Int=3){
    val scheduler=Executors.newScheduledThreadPool(1){ Thread.ofVirtual().unstarted(it) }
   
    poltCtx["TextTranscoder"]=object{
        fun encode(str:String)=str.encodeToByteArray()
        fun decode(str:ByteArray)=str.decodeToString()
    }
    poltCtx["Async"] = object {
        fun timeout(delayMillis:Long,fn:()->Unit){
            scheduler.schedule(
                { fn() },
                delayMillis,
                TimeUnit.MILLISECONDS
            )
        }
    }
    poltCtx["shared"]=ConcurrentHashMap<String,Any?>()
    poltCtx["Concurrent"]=object{
        fun hashmap()=ConcurrentHashMap<String,Any?>()
        fun array()=CopyOnWriteArrayList<Any?>()
    }
    poltCtx["IO"]=object{
        fun Path(path:String)=java.nio.file.Paths.get(path)
        fun File(path:String)=java.io.File(path)

        fun fetch(url:String,method:String="GET",body:String="",headers:Map<String,String> =mapOf()):HttpResponse<ByteArray>{
            val client = HttpClient.newHttpClient()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method(method, BodyPublishers.ofString(body))
            for((header,value) in headers)request.setHeader(header, value)

            val response=client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray())
            return response
        }
    }
    poltCtx["ContentHandlers"]=object{
        fun auto(sock:HttpSocket){
            handler(sock)
        }
        fun error(sock:HttpSocket,code:Int,status:String="",message:String="",error:String=""){
            errorHandler(sock, code,status,message,error)
        }
        fun directory(sock:HttpSocket,path:java.nio.file.Path){
            dirHandler(sock, path)
        }
        fun file(sock:HttpSocket,path:java.nio.file.Path){
            fileHandler(sock, path)
        }
    }

    // poltCtx["Async"]=object{
    //     fun timeout(time:Long):CompletableFuture<Void>{
    //         val future = CompletableFuture<Void>()
    //         CompletableFuture.runAsync({
    //             future.complete(null)
    //         }, CompletableFuture.delayedExecutor(time, TimeUnit.MILLISECONDS))
    //         return future
    //         // return CompletableFuture.supplyAsync {
    //         //     Thread.sleep(time)
    //         //     null
    //         // }
    //     }
    // }

    // makes sure atleast 1 context is already loaded
    val script=Paths.get("./preload.js").normalize().toFile()

    val start=Instant.now()

    println("\u001b[96mwarming up $warmup script contexts\u001b[0m")
    for(i in 0 until warmup) {
        val ctx=ScriptContext()
        scpool.add(ctx)
        ctx.runScript(FakeHttpSocket(), script, "js", mapOf())
        
        val end=Instant.now()
        val diff=Duration.between(start, end)
        println("\u001b[94mwarmed up $i and added to pool\u001b[0m [${diff.toMinutes()}m ${diff.toSeconds()%60}s ${diff.toMillis()%1000}ms]")
    }
}