package dev.samicpp.web

import dev.samicpp.http.HttpSocket
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
// import org.graalvm.polyglot.Engine
import java.io.File
import java.util.concurrent.locks.ReentrantLock


// internal val polyCtx:Context=Context.newBuilder()/*.allowHostAccess(HostAccess.ALL)*//*.allowIO(true)*/.allowAllAccess(true).build()
// private val ctxLock = java.util.concurrent.locks.ReentrantLock()

internal val langs=listOf("js","python")
internal val poltCtx=mutableMapOf<String,Any>()
internal val scpool=mutableListOf<ScriptContext>()
internal var maxPools=100

class ScriptContext(){
    // private val engine=org.graalvm.polyglot.Engine.create()
    private val context=Context.newBuilder()/*.engine(engine)*/.allowAllAccess(true).allowExperimentalOptions(true).build()
    private var locked=false
    private val lock=ReentrantLock()
    val isLocked get()=locked
    init{
        for ((name,prop) in poltCtx){
            for (language in langs)context.getBindings(language).putMember(name,prop)
        }
    }
    fun runScript(socket:HttpSocket,file:File,language:String,customEnv:Map<String,Any>):Value{
        val code = file.readText(Charsets.UTF_8)
        // ctxLock.lock()
        lock.lock()
        locked=true
        try{
            // getBindings(language)
            val bindings=context.getBindings(language)
            bindings.putMember("socket",socket)
            for((name,value) in customEnv)bindings.putMember(name,value)
            val result=context.eval(language,code)
            return result
        } finally {
            lock.unlock()
            locked=false
        }
    }
}

fun execute(socket:HttpSocket,file:File,language:String="js",customEnv:Map<String,Any> =mapOf()):Value{
    for(ctxi in 0..<scpool.size){
        val ctx=scpool[ctxi]
        if(ctx.isLocked)continue
        println("\u001b[32mfound engine context in pool ($ctxi)\u001b[0m")
        return ctx.runScript(socket, file, language, customEnv)
    }
    if(scpool.size<maxPools){
        val ctx=ScriptContext()
        scpool.add(ctx)
        println("no available contexts\n\u001b[33madding engine context to pool (${scpool.size-1})\u001b[0m")
        return ctx.runScript(socket, file, language, customEnv)
    } else {
        println("\u001b[31mwaiting for available engine context in pool\u001b[0m")
        while(true){
            for(ctxi in 0..<scpool.size){
                val ctx=scpool[ctxi]
                if(ctx.isLocked)continue
                return ctx.runScript(socket, file, language, customEnv)
            }
        }
    }
}
