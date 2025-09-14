package dev.samicpp.web

import dev.samicpp.http.HttpSocket
import dev.samicpp.web.polyCtx
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Value
import java.io.File

val polyCtx=Context.newBuilder()/*.allowHostAccess(HostAccess.ALL)*//*.allowIO(true)*/.allowAllAccess(true).build()
private val ctxLock = java.util.concurrent.locks.ReentrantLock()

fun execute(socket:HttpSocket,file:File,language:String="js"):Value{
    val code = file.readText(Charsets.UTF_8)
    ctxLock.lock()
    try{
        // getBindings(language)
        polyCtx.getBindings(language).putMember("socket",socket)
        val result=polyCtx.eval(language,code)
        return result
    } finally {
        ctxLock.unlock()
    }
}
