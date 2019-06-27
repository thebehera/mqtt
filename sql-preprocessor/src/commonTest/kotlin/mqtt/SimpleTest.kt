package mqtt

import mqtt.sql.*
import mqtt.sql.ForeignKeyActions.Cascade
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class SimpleTest {
    @Test
    fun additionWorks() = assertEquals(4, 2 + 2)

    @Test
    fun ensureSimpleTableCreation() {
        @SQLTable
        data class Yolo(val x: String = "x")

    }

    @SQLTable
    interface FlatObjectKeyed {
        @PrimaryKey
        val x: Int
        val y: String
    }

    @SQLTable
    interface ObjectWithPrimaryKeyObjectReference {
        val child: PrimaryKeyChild
    }

    interface PrimaryKeyChild {
        @PrimaryKey
        @ForeignKey(Queued::class, "queuedObject", Cascade)
        val identifier: String
        val value: String
    }

    @SQLTable
    interface ObjectWithNonKeyedChild {
        val child: NonKeyedChild
    }

    @SQLTable
    interface NonKeyedChild {
        @ForeignKey(Queued::class, "queuedObject", Cascade)
        val mqtt_messageId: UShort
        // generate an mqtt id field to track this
        val value: Int
    }

    @SQLTable
    interface ObjectWithCollectionReference {
        val collection: Collection<String>
    }

    @SQLTable
    interface ObjectWithMapReference<Key, Value> {
        val map: Map<Key, Value>
        val value: String
    }

    @SQLTable
    interface Queued {
        @Unique
        @Check("messageId BETWEEN 1 AND ${UShort.MAX_VALUE}")
        val messageId: UShort
        @PrimaryKey
        val queuedObject: Any
    }

    @SQLTable
    interface Vehicle {
        val pistonCount: UShort
        @PrimaryKey
        val identificationNumber: String
    }

    @SQLTable
    interface Car : Vehicle {
        val licensePlate: String
        val state: String
    }


    @SQLTable
    interface CommercialTruck : Vehicle {
        val usDotNumber: String
    }


    @Test
    fun main() {
        println(createTable<Queued>({
            if (it.name == "queuedObject") {
                "TEXT"
            } else {
                null
            }
        }))
        println(createTable<NonKeyedChild>())
        println(createTable<PrimaryKeyChild>())
    }
}
