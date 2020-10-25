package mqtt.persistence

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class JsObjectStore(private val db: IDBDatabase, override val name: String, override val rowData: Row) : PlatformTable {

    override suspend fun upsert(vararg column: Column): Long {
        val objectFromColumn = column.toKeyPair()
        val obj = entriesToObject(objectFromColumn)
        return suspendCoroutine {
            val transaction = db.transaction(name, "readwrite")
            val store = transaction.objectStore(name)
            val request = store.add(obj)
            request.onsuccess = { event ->
                it.resume(event.target.asDynamic().result.unsafeCast<Long>())
            }
            request.onerror = {event ->
                 it.resumeWithException(RuntimeException(request.error.toString()))
            }
        }
    }

    fun entriesToObject(objectFromColumn: Map<String, Any?>): Any {
        val map2 = js("new Map()") as JsMap<Any>
        objectFromColumn.forEach { (key, value) ->
            map2.set(key, value)
        }
        return Object.fromEntries(map2)
    }

    override suspend fun read(rowId: Long): Collection<Column> {
        val transaction = db.transaction(name)
        val request = transaction.objectStore(name).get(rowId)
        return suspendCoroutine {
            val columns = LinkedHashSet<Column>()
            request.onerror = { event ->
                it.resumeWithException(RuntimeException(request.error.toString()))
            }
            request.onsuccess = { event ->
                val resultObj = request.result!!
                val keys = Object.keys(resultObj)
                val values = Object.values(resultObj)
                (keys.indices).forEach { index ->
                    val key = keys[index]
                    val value = values[index]
                    columns += when (jsTypeOf(value)) {
                        "number" -> {
                            if (value.toString().contains('.')) {
                                FloatColumn(key, value)
                            } else {
                                IntegerColumn(key, value)
                            }
                        }
                        "string" -> TextColumn(key, value)
                        else -> TextColumn(key)
                    }
                }
                it.resume(columns)
            }
        }
    }

}
