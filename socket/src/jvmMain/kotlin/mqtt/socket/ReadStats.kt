@file:Suppress("EXPERIMENTAL_API_USAGE")

package mqtt.socket

actual suspend fun readStats(port: UShort, contains: String): List<String> {
    try {
        val process = ProcessBuilder()
            .command("lsof", "-iTCP:${port}", "-sTCP:$contains", "-l", "-n")
            .redirectErrorStream(true)
            .start()
        try {
            process.inputStream.use { stream ->
                return String(stream.readBytes()).split("\n").filter { it.isNotBlank() }
                    .filter { it.contains(contains) }
            }
        } finally {
            process.destroy()
        }
    } catch (t: Throwable) {
        return emptyList()
    }
}