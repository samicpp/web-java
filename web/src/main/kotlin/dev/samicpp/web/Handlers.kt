package dev.samicpp.web

import dev.samicpp.http.HttpSocket
import dev.samicpp.http.Compression as HttpCompression
import java.nio.file.Paths
import java.nio.file.Path
import java.nio.file.Files
import java.io.StringWriter
import java.io.PrintWriter
// import java.io.File
import kotlin.io.path.name
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlinx.serialization.json.*

fun handler(sock:HttpSocket){
    sock.client.apply { 
        println("\u001b[36mclient {  \n   path: $path, \n   method: $method, \n   version: $version, \n   headers: $headers, \n   body[${body.size}]: \"${body.decodeToString()}\", \n}\u001b[0m")
    }   

    var routerPath:String?=null
    var basePath="/"

    val jconf=Paths.get("$serve_dir/config.json")
    if(Files.exists(jconf)){
        val map: Map<String, List<String>> = Json.decodeFromString(jconf.readText())
        val host=sock.client.host
        var key="default"
        if(host in map)key=host
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

    } else if (Files.exists(full_path)) {
        when {
            Files.exists(jconf)&&Files.isSameFile(jconf,full_path)->{
                errorHandler(sock, 403)
            }
            Files.isDirectory(full_path)->{
                dirHandler(sock, full_path)
            }
            Files.isRegularFile(full_path)->{
                fileHandler(sock, full_path)
            }
            else->{
                println("cannot handle file ${full_path.toString()}")
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
        sock.setHeader("Content-Type", "text/html")
    } else if(fileName.endsWith(".poly.py")) {
        isScript="python"
        sock.setHeader("Content-Type", "text/html")
    } else if(def!=null) {
        sock.setHeader("Content-Type", def)
    } else {
        sock.setHeader("Content-Type", "application/octet-stream")
    }

    if(isScript==null){
        sock.close(file.readBytes())
    } else {
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
