package dev.samicpp.web

// import java.util.concurrent.CompletableFuture
// import java.util.concurrent.Executors
// import java.util.concurrent.TimeUnit


fun setup(){
    poltCtx["TextTranscoder"]=object{
        fun encode(str:String)=str.encodeToByteArray()
        fun decode(str:ByteArray)=str.decodeToString()
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