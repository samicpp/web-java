# web-java
java web server using samicpp/java-http

uses GraalVM JDK24 and has not been tested anywhere else

## Polyglot
This webserver supports executing JavaScript and Python code.


## TODO
- [x] serve files
- [x] search apropriate file when requests points to directory
- [ ] allow configuring everything about the server
- [x] support tls
- [x] support dynamic content
- [x] support JavaScript and Python scripting through GraalVM polyglot
- [ ] add option for searching similar files when file doesnt exist
- [x] allow http2 use through alpn
- [ ] ~~implement h2c upgrade~~
- [x] support Java jar/class plugins

