package mqtt.sql

@SQLTable
interface GenericChild<Type> {
    val child: Type
}

@SQLTable
interface MapChildGeneric<Key, Value> {
    val map: Map<Key, Value>
}

@SQLTable
interface CollectionChildGeneric<Value> {
    val collection: Collection<Value>
}