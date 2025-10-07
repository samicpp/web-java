package dev.samicpp.web;

// in plain java only for the sake of testing interop
import java.time.Instant;

public class Debug {
    Instant now=Instant.now();
    public static void start() {
        System.out.println("debug called");
    }
}
