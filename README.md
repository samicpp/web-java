# web-java
java web server using samicpp/java-http

uses GraalVM JDK24 and has not been tested anywhere else

## Polyglot
This server supports executing JavaScript and Python code.
note: you cannot freely pass some values around as that may trigger a thread access violation

## TODO
- [x] serve files
- [x] search apropriate file when requests points to directory
- [ ] allow configuring everything about the server
- [x] support tls
- [x] support dynamic content
- [ ] add option for searching similar files when file doesnt exist
- [x] allow http2 use through alpn
- [ ] ~~implement h2c upgrade~~
