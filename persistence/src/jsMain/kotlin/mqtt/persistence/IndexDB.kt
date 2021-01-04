package mqtt.persistence

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget

fun main() {
    GlobalScope.promise {
        setup()
    }
}

fun setup() {
    val isNodeJs = isNodeJs()
    println("setup")
    if (isNodeJs) {
        js("var setGlobalVars = require('indexeddbshim')")
        js("setGlobalVars(null, {checkOrigin: false})")
        js("global.window = global")
    }
}

/**
 * Exposes the JavaScript [IDBRequest](https://developer.mozilla.org/en/docs/Web/API/IDBRequest) to Kotlin
 */
abstract external class IDBRequest : EventTarget {
    open val result: Any?
    open val error: dynamic
    open val source: UnionIDBCursorOrIDBIndexOrIDBObjectStore?
    open val transaction: IDBTransaction?
    open val readyState: IDBRequestReadyState
    open var onsuccess: ((Event) -> dynamic)?
    open var onerror: ((Event) -> dynamic)?
}

/**
 * Exposes the JavaScript [IDBOpenDBRequest](https://developer.mozilla.org/en/docs/Web/API/IDBOpenDBRequest) to Kotlin
 */
abstract external class IDBOpenDBRequest : IDBRequest {
    open var onblocked: ((Event) -> dynamic)?
    open var onupgradeneeded: ((Event) -> dynamic)?
}

/**
 * Exposes the JavaScript [IDBVersionChangeEvent](https://developer.mozilla.org/en/docs/Web/API/IDBVersionChangeEvent) to Kotlin
 */
open external class IDBVersionChangeEvent : Event {
    open val oldVersion: Int
    open val newVersion: Int?
}

/**
 * Exposes the JavaScript [IDBFactory](https://developer.mozilla.org/en/docs/Web/API/IDBFactory) to Kotlin
 */
abstract external class IDBFactory {
    fun open(name: String, version: Int = definedExternally): IDBOpenDBRequest
    fun deleteDatabase(name: String): IDBOpenDBRequest
    fun cmp(first: Any?, second: Any?): Short
}

external interface DOMStringList {
    val length: Int
    fun item(index: Int): String?
}

external interface ReadonlyMap<T> {
    fun get(key: String): T?
    fun has(key: String): Boolean
    fun forEach(action: (value: T, key: String) -> Unit)
    val size: Int
    fun keys(): Iterator<String>
    fun values(): Iterator<T>
    fun entries(): Iterator<Any>
}

/** ES6 Map interface. */
@JsName("Map")
external interface JsMap<T> : ReadonlyMap<T> {
    fun set(key: String, value: T?)
    fun delete(key: String): Boolean
    fun clear()
}

external class Object {
    companion object {
        fun entries(obj: Any): Any
        fun keys(obj: Any): Array<String>
        fun values(obj: Any): Array<Any?>
        fun fromEntries(map: JsMap<Any>): Any
    }
}

/**
 * Exposes the JavaScript [IDBDatabase](https://developer.mozilla.org/en/docs/Web/API/IDBDatabase) to Kotlin
 */
abstract external class IDBDatabase : EventTarget {
    open val name: String
    open val version: Int
    open val objectStoreNames: dynamic
    open var onabort: ((Event) -> dynamic)?
    open var onclose: ((Event) -> dynamic)?
    open var onerror: ((Event) -> dynamic)?
    open var onversionchange: ((Event) -> dynamic)?
    fun transaction(storeNames: dynamic, mode: IDBTransactionMode = definedExternally): IDBTransaction
    fun transaction(
        storeNames: dynamic,
        mode: String = definedExternally,
        options: String = definedExternally
    ): IDBTransaction

    fun close(): Unit
    fun createObjectStore(name: String, options: IDBObjectStoreParameters = definedExternally): IDBObjectStore
    fun deleteObjectStore(name: String): Unit
}

external interface IDBObjectStoreParameters {
    var keyPath: dynamic /* = null */
        get() = definedExternally
        set(value) = definedExternally
    var autoIncrement: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

//@kotlin.internal.InlineOnly
inline fun IDBObjectStoreParameters(
    keyPath: dynamic = null,
    autoIncrement: Boolean? = false
): IDBObjectStoreParameters {
    val o = js("({})")

    o["keyPath"] = keyPath
    o["autoIncrement"] = autoIncrement

    return o
}

/**
 * Exposes the JavaScript [IDBObjectStore](https://developer.mozilla.org/en/docs/Web/API/IDBObjectStore) to Kotlin
 */
abstract external class IDBObjectStore : UnionIDBCursorOrIDBIndexOrIDBObjectStore,
    UnionIDBIndexOrIDBObjectStore {
    open var name: String
    open val keyPath: Any?
    open val indexNames: dynamic
    open val transaction: IDBTransaction
    open val autoIncrement: Boolean
    fun put(value: Any?, key: Any? = definedExternally): IDBRequest
    fun add(value: Any?, key: Any? = definedExternally): IDBRequest
    fun delete(query: Any?): IDBRequest
    fun clear(): IDBRequest
    fun get(query: Any?): IDBRequest
    fun getKey(query: Any?): IDBRequest
    fun getAll(query: Any? = definedExternally, count: Int = definedExternally): IDBRequest
    fun getAllKeys(query: Any? = definedExternally, count: Int = definedExternally): IDBRequest
    fun count(query: Any? = definedExternally): IDBRequest
    fun openCursor(query: Any? = definedExternally, direction: IDBCursorDirection = definedExternally): IDBRequest
    fun openKeyCursor(query: Any? = definedExternally, direction: IDBCursorDirection = definedExternally): IDBRequest
    fun index(name: String): IDBIndex
    fun createIndex(name: String, keyPath: dynamic, options: IDBIndexParameters = definedExternally): IDBIndex
    fun deleteIndex(name: String): Unit
}

external interface IDBIndexParameters {
    var unique: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var multiEntry: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

//@kotlin.internal.InlineOnly
inline fun IDBIndexParameters(unique: Boolean? = false, multiEntry: Boolean? = false): IDBIndexParameters {
    val o = js("({})")

    o["unique"] = unique
    o["multiEntry"] = multiEntry

    return o
}

/**
 * Exposes the JavaScript [IDBIndex](https://developer.mozilla.org/en/docs/Web/API/IDBIndex) to Kotlin
 */
abstract external class IDBIndex : UnionIDBCursorOrIDBIndexOrIDBObjectStore, UnionIDBIndexOrIDBObjectStore {
    open var name: String
    open val objectStore: IDBObjectStore
    open val keyPath: Any?
    open val multiEntry: Boolean
    open val unique: Boolean
    fun get(query: Any?): IDBRequest
    fun getKey(query: Any?): IDBRequest
    fun getAll(query: Any? = definedExternally, count: Int = definedExternally): IDBRequest
    fun getAllKeys(query: Any? = definedExternally, count: Int = definedExternally): IDBRequest
    fun count(query: Any? = definedExternally): IDBRequest
    fun openCursor(query: Any? = definedExternally, direction: IDBCursorDirection = definedExternally): IDBRequest
    fun openKeyCursor(query: Any? = definedExternally, direction: IDBCursorDirection = definedExternally): IDBRequest
}

/**
 * Exposes the JavaScript [IDBKeyRange](https://developer.mozilla.org/en/docs/Web/API/IDBKeyRange) to Kotlin
 */
abstract external class IDBKeyRange {
    open val lower: Any?
    open val upper: Any?
    open val lowerOpen: Boolean
    open val upperOpen: Boolean
    fun _includes(key: Any?): Boolean

    companion object {
        fun only(value: Any?): IDBKeyRange
        fun lowerBound(lower: Any?, open: Boolean = definedExternally): IDBKeyRange
        fun upperBound(upper: Any?, open: Boolean = definedExternally): IDBKeyRange
        fun bound(
            lower: Any?,
            upper: Any?,
            lowerOpen: Boolean = definedExternally,
            upperOpen: Boolean = definedExternally
        ): IDBKeyRange
    }
}

/**
 * Exposes the JavaScript [IDBCursor](https://developer.mozilla.org/en/docs/Web/API/IDBCursor) to Kotlin
 */
abstract external class IDBCursor : UnionIDBCursorOrIDBIndexOrIDBObjectStore {
    open val source: UnionIDBIndexOrIDBObjectStore
    open val direction: IDBCursorDirection
    open val key: Any?
    open val primaryKey: Any?
    fun advance(count: Int): Unit
    fun `continue`(key: Any?): Unit
    fun continuePrimaryKey(key: Any?, primaryKey: Any?): Unit
    fun update(value: Any?): IDBRequest
    fun delete(): IDBRequest
}

/**
 * Exposes the JavaScript [IDBCursorWithValue](https://developer.mozilla.org/en/docs/Web/API/IDBCursorWithValue) to Kotlin
 */
abstract external class IDBCursorWithValue : IDBCursor {
    open val value: Any?
}

/**
 * Exposes the JavaScript [IDBTransaction](https://developer.mozilla.org/en/docs/Web/API/IDBTransaction) to Kotlin
 */
abstract external class IDBTransaction : EventTarget {
    open val objectStoreNames: dynamic
    open val mode: IDBTransactionMode
    open val db: IDBDatabase
    open val error: dynamic
    open var onabort: ((Event) -> dynamic)?
    open var oncomplete: ((Event) -> dynamic)?
    open var onerror: ((Event) -> dynamic)?
    fun objectStore(name: String): IDBObjectStore
    fun abort(): Unit
}

external interface UnionIDBCursorOrIDBIndexOrIDBObjectStore

external interface UnionIDBIndexOrIDBObjectStore

/* please, don't implement this interface! */
external interface IDBRequestReadyState

/* please, don't implement this interface! */
external interface IDBCursorDirection

/* please, don't implement this interface! */
external interface IDBTransactionMode
