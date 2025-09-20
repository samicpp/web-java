package dev.samicpp.web

import java.util.*
import dev.samicpp.http.hpack.*

// chat gpt 👍
object HpackRoundTripTest {
    // @JvmStatic
    fun main() {
        val encoder = Encoder()
        val decoder = Decoder()

        try {
            println("HPACK round-trip tests starting...")

            // Test 1: Basic headers round-trip
            runTest(
                name = "basic",
                encoder = encoder,
                decoder = decoder,
                headers = listOf(
                    ":method" to "GET",
                    ":scheme" to "http",
                    ":path" to "/",
                    "host" to "www.example.com",
                    "accept-encoding" to "gzip, deflate"
                )
            )

            // Test 2: Dynamic table behaviour (same header repeated across blocks)
            // Use same encoder+decoder instances so dynamic table state is maintained.
            runTest(
                name = "dynamic-first",
                encoder = encoder,
                decoder = decoder,
                headers = listOf(
                    "x-custom-key" to "custom-value"
                )
            )
            // Encode a second block with the same header — encoder should use indexing,
            // decoder should correctly decode it using its dynamic table state.
            runTest(
                name = "dynamic-second",
                encoder = encoder,
                decoder = decoder,
                headers = listOf(
                    "x-custom-key" to "custom-value"
                )
            )

            // Test 3: Huffman-friendly long value (exercise string encoding)
            val longValue = buildString {
                repeat(30) { append("compressible-sequence-") }
            }
            runTest(
                name = "huffman-long-value",
                encoder = encoder,
                decoder = decoder,
                headers = listOf(
                    "x-long-header" to longValue
                )
            )

            println("All HPACK tests passed ✅")
            // exit normally
            return
        } catch (ex: AssertionError) {
            System.err.println("HPACK tests failed: ${ex.message}")
            ex.printStackTrace()
            System.exit(1)
        } catch (ex: Exception) {
            System.err.println("Unexpected error while running HPACK tests: ${ex.message}")
            ex.printStackTrace()
            System.exit(2)
        }
    }

    private fun runTest(name: String, encoder: Encoder, decoder: Decoder, headers: List<Pair<String, String>>) {
        println(" → running test '$name' with ${headers.size} header(s)")
        // encode using encoder
        val encoded = encoder.encode(headers)
        println("   encoded ${encoded.size} bytes: ${toHex(encoded)}")

        // decode using decoder
        val decoded = decoder.decode(encoded)

        // build expected Header objects (the project's Header type)
        val expected = headers.map { dev.samicpp.http.hpack.Header(it.first, it.second) }

        assertHeadersEqual(expected, decoded, name)
        println("   test '$name' passed")
    }

    private fun assertHeadersEqual(expected: List<dev.samicpp.http.hpack.Header>, actual: List<dev.samicpp.http.hpack.Header>, testName: String) {
        if (expected.size != actual.size) {
            throw AssertionError("[$testName] header count mismatch: expected ${expected.size}, got ${actual.size}\n expected=$expected\n actual=$actual")
        }
        for (i in expected.indices) {
            val e = expected[i]
            val a = actual[i]
            if (e.name != a.name || e.value != a.value) {
                throw AssertionError("[$testName] header #$i mismatch:\n expected=(${e.name}: ${e.value})\n actual=(${a.name}: ${a.value})\n full-expected=$expected\n full-actual=$actual")
            }
        }
    }

    // small hex helper for diagnostic printing
    private fun toHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
