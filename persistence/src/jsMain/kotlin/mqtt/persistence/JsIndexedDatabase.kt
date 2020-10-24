package mqtt.persistence

import kotlinx.coroutines.*
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
        IDBDatabase
        val context = CoroutineScope(currentCoroutineContext() + Dispatchers.Unconfined)
        db = suspendCancellableCoroutine { continuation ->
            val indexedDb = js("window.indexedDB").unsafeCast<IDBFactory>()
            val request = indexedDb.open(dbName, 2)
            request.onerror = {
                console.log("error", it)
                continuation.resumeWithException(RuntimeException(request.error.toString()))
            }
            request.onsuccess = {
                val db = it.target.asDynamic().result.unsafeCast<IDBDatabase>()
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
                    println("Create obj store")
                    val objStore = js("db.createObjectStore(name, {autoIncrement: true})")
                        .unsafeCast<IDBObjectStore>()
                    println("obj store $objStore")
                    objectStores[name] = JsObjectStore(db, objStore, name, row)
                }
                println("launch $tables")
                context.launch {
                    objectStores.forEach { (name, objectStore) ->
                        suspendCoroutine<Unit> {
                            objectStore.objectStore.transaction.oncomplete = { _ ->
                                it.resume(Unit)
                            }
                            objectStore.objectStore.transaction.onerror = { event ->
                                it.resumeWithException(RuntimeException(event.toString()))
                            }
                        }
                    }
                    continuation.resume(db)
                }
                println("on upgrade complete")
            }
            console.log("success", request)
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