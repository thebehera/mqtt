println(
    (0 until 65_535).joinToString(
        prefix = "INSERT INTO FreeControlPackets(connectionId, packetIdentifier) VALUES (1, ",
        postfix = ");",
        separator = "),(1, "
    )
)
