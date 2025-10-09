package dev.samicpp.web

import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.jar.JarFile
import java.nio.file.Path
import dev.samicpp.http.HttpSocket

interface HttpHandler{
    // var alive: Boolean;
    fun getAlive(): Boolean;
    fun setAlive(alive: Boolean);

    fun init(self: File);
    fun handle(sock: HttpSocket, handlePath: Path?);
}


fun loadClass(file:File,type:String,/*main:String,sock:HttpSocket,extra:Map<String,Any> =mapOf()*/):Class<*>{
    val url=
    if(type=="jar") file.toURI().toURL()
    else file.parentFile.toURI().toURL()

    println("loading $url")
    val classLoader=URLClassLoader(arrayOf(url), Thread.currentThread().contextClassLoader)

    val className=when{
        type=="jar"->JarFile(file).manifest.mainAttributes.getValue("Main-Class")
        type=="class"->file.nameWithoutExtension
        else->file.name
    }

    val clazz=classLoader.loadClass(className)
    // val instance=clazz.getDeclaredConstructor().newInstance()
    // val method=clazz.getMethod(main, sock.javaClass, extra.javaClass)

    // method.invoke(instance, sock, extra)
    return clazz
}
