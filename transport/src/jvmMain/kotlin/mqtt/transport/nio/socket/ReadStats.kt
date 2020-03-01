package mqtt.transport.nio.socket

actual fun readStats(port: UShort, contains: String): List<String> {
    val process = ProcessBuilder()
        .command("lsof", "-iTCP:${port}", "-sTCP:$contains", "-l", "-n")
        .redirectErrorStream(true)
        .start()
    try {
        process.inputStream.use { stream ->
            return String(stream.readBytes()).split(System.lineSeparator()).filter { it.isNotBlank() }
                .filter { it.contains(contains) }
        }
    } finally {
        process.destroy()
    }
}