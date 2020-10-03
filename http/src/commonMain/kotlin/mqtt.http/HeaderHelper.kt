package mqtt.http

fun Map<String, String>.toHeaders(): Map<CharSequence, Set<CharSequence>> {
    val map = LinkedHashMap<CharSequence, LinkedHashSet<CharSequence>>(size)
    forEach { (key, value) ->
        val set = map.getOrPut(key) { LinkedHashSet() }
        set += value
    }
    return map
}