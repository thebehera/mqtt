package mqtt.persistence

import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class JsIndexedDatabase(private val dbName: String): PlatformDatabase {
    private var db: IDBDatabase? = null
    private val objectStores = mutableMapOf<String, JsObjectStore>()

    override suspend fun open(tables: Map<String, Row>): Map<String, JsObjectStore> {
        if (db != null) return objectStores
        if (!isDbSetup) {
            setup()
            isDbSetup = true
        }
        val operations = mutableListOf<IDBObjectStore>()
        val indexedDb =
            window.asDynamic().indexedDB.unsafeCast<IDBFactory>()
        db = suspendCancellableCoroutine { continuation ->
            val request = indexedDb.open(dbName, 3)
            request.onerror = {
                console.log("error", it)
                continuation.resumeWithException(RuntimeException(request.error.toString()))
            }
            request.onsuccess = {
                val db = it.target.asDynamic().result.unsafeCast<IDBDatabase>()
                val names = db.objectStoreNames.unsafeCast<DOMStringList>()
                val tableNames = mutableListOf<String>()
                val count = names.length
                (0..count).forEachIndexed { index, _->
                    val name = names.item(index)
                    if (name != null) {
                        tableNames += name
                    }
                }
                tableNames.forEach { name ->
                    val row = tables[name]!!
                    objectStores[name] = JsObjectStore(db, name, row)
                }
                continuation.resume(db)
            }
            request.onblocked = {
                console.log("on blocked", it)
            }
            request.onupgradeneeded = {
                val db = it.target.asDynamic().result.unsafeCast<IDBDatabase>()
                db.onabort = {
                    console.log("abort error", it)
                    continuation.resumeWithException(RuntimeException("Abort error $it"))
                }
                request.onerror = {
                    console.log("error", it)
                    continuation.resumeWithException(RuntimeException(request.error.toString()))
                }
                tables.forEach { (name, row) ->
                    val objStore = db.createObjectStore(name, IDBObjectStoreParameters("rowId", true))
                        .unsafeCast<IDBObjectStore>()
                    operations += objStore
                    objectStores[name] = JsObjectStore(db, name, row)
                }
            }
        }
        return objectStores
    }

    override suspend fun createTable(name: String, rowData: Row): PlatformTable {
        throw UnsupportedOperationException()
    }

    override suspend fun dropTable(table: PlatformTable) {}


    companion object {
        private var isDbSetup = false
    }
}