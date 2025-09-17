package dev.samicpp.web

// import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


fun setup(){
    poltCtx["TextTranscoder"]=object{
        fun encode(str:String)=str.encodeToByteArray()
        fun decode(str:ByteArray)=str.decodeToString()
    }

    val scheduler=Executors.newScheduledThreadPool(1){ Thread.ofVirtual().unstarted(it) }

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