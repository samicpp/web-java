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
import java.time.Duration
import java.util.regex.Pattern
import java.net.InetSocketAddress
// import java.io.File
import kotlin.io.path.name
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.text.startsWith
import kotlin.collections.findLast
import kotlinx.serialization.json.*

fun httpDate():String {
    val now=ZonedDateTime.now(ZoneOffset.UTC)
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(now)
}

fun handler(sock:HttpSocket){
    val now=Instant.now()
    println("\u001b[35minvoked handler at \u001b[1m\u001b[95m$now\u001b[0m")
    sock.client.apply { 
        println("\u001b[36mclient {\n   path: $path, \n   method: $method, \n   version: $version, \n   headers: $headers, \n   body[${body.size}]: \"${body.decodeToString()}\", \n   host: $host, \n   isHttps: ${sock.isHttps()}, \n}\u001b[0m")
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
    
    val userAgent=sock.client.headers["user-agent"]?.get(0)

    if(userAgent?.startsWith("python-requests")==true||userAgent?.startsWith("l9tcpid")==true){
        println("determined client is a malicious bot")
        println("closing connection early")
        val buff="${sock.client.address}\n".encodeToByteArray()
        sock.close(buff)
    }
    
    // TODO: make this configurable
    val seperatorsInfront=listOf(">>","%3E%3E")
    val seperatorsBehind=listOf("?",":","#")
    var clientPath="${sock.client.path}"
    for(sep in seperatorsInfront)clientPath=clientPath.split(sep).last()
    for(sep in seperatorsBehind)clientPath=clientPath.split(sep).first()

    // TODO: cache config until change
    val jconf=Paths.get("$serve_dir/host-routes.json")
    if(Files.exists(jconf)){
        val map: Map<String, List<String>> = Json.decodeFromString(jconf.readText())
        val scheme=if(sock.isHttps())"https://" else "http://"
        val host=sock.client.host
        var key="default"

        if(scheme+host+clientPath in map)key=scheme+host+clientPath
        if(scheme+host in map)key=scheme+host
        else if(host in map)key=host
        else {
            match@
            for((entry,_) in map){
                val reg=Pattern.compile(entry)
                // println("mathcing $reg against ${scheme+host+sock.client.path}")
                if(reg.matcher(scheme+host+clientPath).matches()){
                    key=entry
                    // println("found match $reg")
                    break@match
                }
            }
        }
        basePath="/${map[key]!![0]}"

        routerPath=map[key]?.getOrNull(1)
        // println(map)
        // println(host)
    }
    // val full_path_str="${sock.client.path}"
    var full_path_tmp="$serve_dir/$basePath/$clientPath"
        full_path_tmp=full_path_tmp.replace(Regex("\\?.*"), "")
        full_path_tmp=full_path_tmp.replace(Regex("\\/\\.{1,2}(?=\\/|$)"), "/")
        full_path_tmp=full_path_tmp.replace(Regex("\\/+"), "/")
    val full_path=Paths.get(full_path_tmp).normalize()

    val router:Path?=
    if(routerPath!=null) Paths.get("$serve_dir/$basePath/$routerPath").normalize()
    else null
    // println("full path = $full_path\nstring ver = $full_path_str\nfinal ver = $full_path_tmp")

    if((sock.client.headers["accept-encoding"]?.get(0)?:"").contains("gzip"))sock.compression=HttpCompression.Gzip

    if(router!=null&&Files.exists(router)&&Files.isRegularFile(router)) {
        
        fileHandler(sock, router, mapOf("handlePath" to full_path))

    } else if (Files.exists(full_path)&&Files.exists(jconf)&&Files.isSameFile(jconf,full_path)) {
        errorHandler(sock, 403)
    } else {
        fileDirErr(sock, full_path)
    }

    val end=Instant.now()
    val diff=Duration.between(now,end)
    println("\u001b[35mhandler finished after \u001b[1m\u001b[95m[${diff.toMinutes()}m ${diff.toSeconds()%60}s ${diff.toMillis()%1000}ms]\u001b[0m")
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
        println("resource does not exist $path")
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
fun fileHandler(sock:HttpSocket,path:Path,scriptExtras:Map<String,Any> =mapOf()){
    val file=path.toFile()
    val fileName=path.fileName.toString()
    val last=fileName.split(".").last()
    val def=mimeMap[last]
    var isScript:String?=null
    var isSpecial:String?=null

    sock.status=200
    sock.statusMessage="OK"

    if(fileName.endsWith(".poly.js")) {
        isScript="js"
    } else if(fileName.endsWith(".poly.py")) {
        isScript="python"
    } else if(fileName.endsWith(".link")) {
        isSpecial="link"
    } else if(fileName.endsWith(".redirect")) {
        isSpecial="redirect"
    } else if(fileName.contains(".var.")) {
        isSpecial="var"
        if(def!=null)sock.setHeader("Content-Type", "$def; charset=utf-8")
    } else if(def!=null) {
        if(def.startsWith("text")) sock.setHeader("Content-Type", "$def; charset=utf-8")
        else sock.setHeader("Content-Type", def)
    } else {
        sock.setHeader("Content-Type", "application/octet-stream")
    }

    // println("file located at ${file.absolutePath}")

    if(isScript!=null){
        println("handling script")
        sock.setHeader("Content-Type", "text/html; charset=utf-8")
        // sock.setHeader("Cache-Control", "public, max-age=5")
        try{
            val env=mutableMapOf<String,Any>("scriptPath" to file.absolutePath)
            for((k,v) in scriptExtras)env[k]=v
            execute(sock, file, isScript, env)
        }catch(err: Exception){
            val sw=StringWriter()
            val pw=PrintWriter(sw)
            err.printStackTrace(pw)
            val serr=sw.toString()

            println("\u001b[31mscript error\u001b[0m\n$serr")
            if(!sock.sentHeaders)errorHandler(sock, 500, "Internal Server Error", "Script Errored\n", serr)
        }
    } else if(isSpecial!=null) {
        println("file is special file $isSpecial")
        val vars=mapOf(
            "%PATH%" to sock.client.path,
            "%HOST%" to sock.client.host,
            "%SCHEME%" to if(sock.isHttps())"https://" else "http://",
            "%IP%" to (sock.client.address as InetSocketAddress).hostName,
            "%FULL_IP%" to sock.client.address.toString(),
        )
        when(isSpecial){
            "link"->{
                val pathString=file.readText()
                val newPath=Paths.get(pathString).normalize()
                println("handling new provided path $newPath")
                fileDirErr(sock, newPath)
            }
            "redirect"->{
                var url=file.readText()
                for((vari,value) in vars)url=url.replace(vari,value)
                sock.status=302
                sock.statusMessage="Found"
                sock.setHeader("Location", url)
                println("sending off response to ${sock.client.path}")
                sock.close()
            }
            "var"->{
                var text=file.readText()
                for((vari,value) in vars)text=text.replace(vari,value)
                println("sending off response to ${sock.client.path}")
                sock.close(text)
            }
        }
    } else {
        println("sending static file")
        // sock.setHeader("Cache-Control", "public, max-age=15")
        sock.close(file.readBytes())
    }

    println("fileHandler returned intact")
}
