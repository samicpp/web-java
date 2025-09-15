// const sched=Polyglot.import("scheduler")

!function(){
    const client=socket.getClient()
    const headers={...client.getHeaders()}

    // 
    console.log(JSON.stringify(headers))
    if(headers.upgrade?.[0]=="websocket"){
        const websocket=socket.websocket()
        while(true){
            const inc=websocket.incoming()
            if(inc.length==0)break
            // websocket.sendText("craxzy")
            // console.log("client websocket loop")
            // console.log(Object.keys(websocket))
            for(const frame of inc){
                console.log(Object.keys(frame))
                const type=frame.getStringType()
                const payload=frame.getPayload()
                console.log(`client sent a ${type} frame sized ${payload.length}`)
                switch(type.toLowerCase()){
                    case"text":
                        websocket.sendText("crazy: "+TextTranscoder.decode(payload))
                        break
                    case"binary":
                        websocket.sendBinary("crazy: "+TextTranscoder.decode(payload))
                        break
                    case"ping":
                        websocket.sendPong(payload)
                        break
                    case"connectionclose":
                        const code=(payload[0]&0xff)<<8|payload[1]&0xff
                        console.log(payload)
                        const rbuff=payload.slice(2)
                        const reason=TextTranscoder.decode(rbuff)
                        console.log(`client wants to disconnect ${code} cause: ${reason}`)
                        websocket.sendClose(code,"stop")
                        websocket.close()
                        break
                }
            }
            // websocket.sendClose(1001,"stop")
            // websocket.close()
        }
        console.log("client disconnected")
        // loop
    } else {
        socket.setHeader("Content-Type","text/plain")
        socket.close("try again with websocket")
    }
}();
