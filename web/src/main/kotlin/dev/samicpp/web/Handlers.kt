package dev.samicpp.web

import dev.samicpp.http.HttpSocket
import dev.samicpp.http.Compression as HttpCompression
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files
import java.io.StringWriter
import java.io.PrintWriter
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.Instant
import java.util.regex.Pattern
// import java.io.File
import kotlin.io.path.name
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.text.startsWith
import kotlinx.serialization.json.*

fun httpDate():String {
    val now=ZonedDateTime.now(ZoneOffset.UTC)
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(now)
}

fun handler(sock:HttpSocket){
    println("\u001b[35minvoked handler at \u001b[1m\u001b[95m${Instant.now()}\u001b[0m")
    sock.client.apply { 
        println("\u001b[36mclient {\n   path: $path, \n   method: $method, \n   version: $version, \n   headers: $headers, \n   body[${body.size}]: \"${body.decodeToString()}\", \n   host: $host, \n}\u001b[0m")
    }   

    var routerPath:String?=null
    var basePath="/"

    val headers=mapOf(
        "Date" to httpDate(),
        "Server" to "ChromeCast-BackgroundMediaServer/94.1337.666",
        "X-Content-Type-Options" to "nosniff",
        "X-Powered-By" to "SimpleHTTP/0.6 Python/3.13.7",

        "Access-Control-Allow-Origin" to "*",
        "Access-Control-Allow-Methods" to "GET, POST, PUT, DELETE, OPTIONS",
        "Access-Control-Allow-Headers" to "*",
        "Access-Control-Allow-Credentials" to "true",
        "Access-Control-Allow-Private-Network" to "true",

        "Content-Security-Policy" to "default-src * 'unsafe-inline' 'unsafe-eval' data: blob:; frame-ancestors *;",
        "Permissions-Policy" to "camera=*, microphone=*, geolocation=*, clipboard-read=*, clipboard-write=*, fullscreen=*, accelerometer=*, gyroscope=*, magnetometer=*, payment=*",
    )
    
    for((name,value) in headers) sock.setHeader(name, value)
    

    // TODO: cache config until change
    val jconf=Paths.get("$serve_dir/host-routes.json")
    if(Files.exists(jconf)){
        val map: Map<String, List<String>> = Json.decodeFromString(jconf.readText())
        val scheme=if(sock.isHttps())"https://" else "http://"
        val host=sock.client.host
        var key="default"

        if(scheme+host in map)key=scheme+host
        else if(host in map)key=host
        else {
            match@
            for((entry,_) in map){
                val reg=Pattern.compile(entry)
                if(reg.matcher(scheme+host+sock.client.path).matches()){
                    key=entry
                    break@match
                }
            }
        }
        basePath="/${map[key]!![0]}"

        routerPath=map[key]?.getOrNull(1)
        // println(map)
        // println(host)
    }
    val full_path_str="$serve_dir/$basePath/${sock.client.path}"
    var full_path_tmp=full_path_str.replace(Regex("\\?.*"), "")
        full_path_tmp=full_path_tmp.replace(Regex("\\/\\.{1,2}(?=\\/|$)"), "/")
        full_path_tmp=full_path_tmp.replace(Regex("\\/+"), "/")
    val full_path=Paths.get(full_path_tmp).normalize()

    val router:Path?=
    if(routerPath!=null) Paths.get("$serve_dir/$basePath/$routerPath").normalize()
    else null
    // println("full path = $full_path\nstring ver = $full_path_str\nfinal ver = $full_path_tmp")

    if((sock.client.headers["accept-encoding"]?.get(0)?:"").contains("gzip"))sock.compression=HttpCompression.Gzip

    if(router!=null&&Files.exists(router)&&Files.isRegularFile(router)) {
        
        fileHandler(sock, router)

    } else if (Files.exists(jconf)&&Files.isSameFile(jconf,full_path)) {
        errorHandler(sock, 403)
    } else {
        fileDirErr(sock, full_path)
    }

}

fun fileDirErr(sock:HttpSocket,path:Path){
    if (Files.exists(path)) {
        when {
            Files.isDirectory(path)->{
                dirHandler(sock, path)
            }
            Files.isRegularFile(path)->{
                fileHandler(sock, path)
            }
            else->{
                println("cannot handle file ${path.toString()}")
                errorHandler(sock, 501)
            }
        }
    } else {
        println("resource does not exist")
        errorHandler(sock, 404)
    }
}

fun errorHandler(sock:HttpSocket,code:Int,status:String="",message:String="",error:String=""){
    sock.status=code
    sock.statusMessage=status
    when(code){
        400->{
            sock.statusMessage="Bad Request"
            sock.setHeader("Content-Type", "text/plain")
            sock.close("400 Bad Request")
        }
        403->{
            sock.statusMessage="Forbidden"
            sock.setHeader("Content-Type", "text/plain")
            sock.close("403 Forbidden")
        }
        404->{
            sock.statusMessage="Not Found"
            sock.setHeader("Content-Type", "text/plain")
            sock.close("404 Not Found")
        }
        409->{
            sock.statusMessage="Confilct"
            sock.setHeader("Content-Type", "text/plain")
            sock.close("409 Confilct")
        }
        500->{
            sock.statusMessage="Internal Server Error"
            sock.setHeader("Content-Type", "text/plain")
            sock.close("500 Internal Server Error\n\n$message: $error")
        }
        501->{
            sock.statusMessage="Not Implemented"
            sock.setHeader("Content-Type", "text/plain")
            sock.close("501 Not Implemented")
        }
        else->{
            sock.setHeader("Content-Type", "text/plain")
            sock.close("$message")
        }
    }
}

fun dirHandler(sock:HttpSocket,path:Path){
    val parent=path.fileName.toString()
    var hfile:Path?=null

    for(file in Files.list(path)){
        if(!file.isRegularFile())continue
        val fileName=file.fileName.toString()
        when{
            fileName.startsWith(parent)->{
                hfile=file
                break
            }
            fileName.startsWith("index.")->{
                hfile=file
                break
            }
        }
    }

    if(hfile!=null){
        println("found file $hfile")
        fileHandler(sock, hfile)
    } else {
        println("couldnt find file in $path starting with $parent or index.")
        errorHandler(sock, 409)
    }
}

// TODO: implement custom dynamic `*.dyn.*` files (jsdyn and pydyn)
// TODO: implement custom `*.redirect` files
// TODO: implement custom `*.link` files
fun fileHandler(sock:HttpSocket,path:Path){
    val file=path.toFile()
    val fileName=path.fileName.toString()
    val last=fileName.split(".").last()
    val def=mimeMap[last]
    var isScript:String?=null

    sock.status=200
    sock.statusMessage="OK"

    if(fileName.endsWith(".poly.js")) {
        isScript="js"
    } else if(fileName.endsWith(".poly.py")) {
        isScript="python"
    } else if(def!=null) {
        if(def.startsWith("text")) sock.setHeader("Content-Type", "$def; charset=utf-8")
        else sock.setHeader("Content-Type", def)
    } else {
        sock.setHeader("Content-Type", "application/octet-stream")
    }

    if(isScript==null){
        sock.setHeader("Cache-Control", "public, max-age=15")
        sock.close(file.readBytes())
    } else {
        sock.setHeader("Content-Type", "text/html; charset=utf-8")
        sock.setHeader("Cache-Control", "public, max-age=5")
        try{
            execute(sock, file, isScript, mapOf("scriptPath" to file.absolutePath))
        }catch(err: Exception){
            val sw=StringWriter()
            val pw=PrintWriter(sw)
            err.printStackTrace(pw)
            val serr=sw.toString()

            println("\u001b[31mscript error\u001b[0m\n$serr")
            if(!sock.sentHeaders)errorHandler(sock, 500, "Internal Server Error", "Script Errored\n", serr)
        }
    }
}
