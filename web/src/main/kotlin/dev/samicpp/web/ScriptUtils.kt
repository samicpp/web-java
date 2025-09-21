package dev.samicpp.web

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


fun setup(){
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
        fun fetch(url:String,method:String="GET",body:String="",headers:List<Pair<String,String>> =listOf()):HttpResponse<String>{
            val client = HttpClient.newHttpClient()

            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method(method, BodyPublishers.ofString(body))
            for((header,value) in headers)request.setHeader(header, value)

            val response=client.send(request.build(), HttpResponse.BodyHandlers.ofString())
            return response
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
}