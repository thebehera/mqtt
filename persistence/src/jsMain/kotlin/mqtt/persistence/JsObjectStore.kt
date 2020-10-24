package mqtt.persistence

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class JsObjectStore(private val db: IDBDatabase, val objectStore: IDBObjectStore,  override val name: String, override val rowData: Row) : PlatformTable {

    override suspend fun upsert(vararg column: Column): Long {
        val objectFromColumn = column.toKeyPair()
        val obj = entriesToObject(objectFromColumn)
        val request = objectStore.add(obj)
        return suspendCoroutine {
            request.onsuccess = { event ->
                it.resume(event.target.asDynamic().result.unsafeCast<Long>())
            }
            request.onerror = {event ->
                 it.resumeWithException(RuntimeException(request.error.toString()))
            }
        }
    }

    fun entriesToObject(objectFromColumn: Map<String, Any?>): Any {
        return js("Object.fromEntries(objectFromColumn)") as Any
    }

    override suspend fun read(rowId: Long): Collection<Column> {
        val request = objectStore.get(rowId)
        return suspendCoroutine {
            val columns = LinkedHashSet<Column>()
            request.onerror = { event ->
                it.resumeWithException(RuntimeException(request.error.toString()))
            }
            request.onsuccess = { event ->
                val resultObj = request.result
                println(resultObj)
                it.resume(columns)
            }
        }
    }

}