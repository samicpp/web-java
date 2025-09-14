// const socket=socket
// socket.close("helloworld")
// const conn=socket
// console.log("type of socket",typeof socket)
// console.log("socket",JSON.stringify(Object.getOwnPropertyDescriptors(socket)))
console.log("global",Object.keys(Object.getOwnPropertyDescriptors(globalThis)))
// conn.close("hellowldf")

!function(conn){
    let c=conn.getClient()
    // console.log("client",c)
    conn.close("helloworld at "+c.getPath())
}(socket)
