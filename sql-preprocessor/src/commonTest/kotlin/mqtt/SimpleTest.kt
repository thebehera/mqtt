package mqtt

import mqtt.sql.*
import mqtt.sql.ForeignKeyActions.Cascade
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalUnsignedTypes
class SimpleTest {
    @Test
    fun additionWorks() = assertEquals(4, 2 + 2)

    @Test
    fun ensureSimpleTableCreation() {
        main()
    }

    @SQLTable
    interface FlatObjectKeyed {
        @PrimaryKey
        val x: Int
        val y: String
    }

    @SQLTable
    interface ObjectWithFlatKeyedReference {
        val child: FlatObjectKeyed
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
        val mqtt_inserted_id: UShort
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
        @ForeignKey(Vehicle::class, "identificationNumber", Cascade)
        val licensePlate: String
        val state: String
    }


    @SQLTable
    interface CommercialTruck : Vehicle {
        @ForeignKey(Vehicle::class, "identificationNumber", Cascade)
        val usDotNumber: String
    }

    @Test
    fun objectsWithChildren() {
        createTableInheritence<Vehicle>(CommercialTruck::class, Car::class).forEach { println(it) }
    }


    @Test
    fun main() {
        println("/* Create Tables for queued objects */")
        println(createTable(Queued::class, {
            if (it.name == "queuedObject") {
                "TEXT"
            } else {
                null
            }
        }))
        println(createTable(NonKeyedChild::class))
        println(createTable(PrimaryKeyChild::class))
        println("\n\n/* Insert into for queued objects */")

        // deleting from the queue deletes from the child table
        println(insertInto<Queued>())
        println(insertInto<NonKeyedChild>())
        println(insertInto<PrimaryKeyChild>())

        println("\n\n/* Create Views for queued objects */")
        // we need to pop from the queue
        /**
         * SELECT `mqtt.simpleTest.Queued`.messageId, `mqtt.SimpleTest.NonKeyedChild`.*
         * FROM `mqtt.simpleTest.Queued`
         * INNER JOIN `mqtt.SimpleTest.NonKeyedChild` ON `mqtt.simpleTest.Queued`.queuedObject = `mqtt.SimpleTest.NonKeyedChild`.mqtt_inserted_id
         * ORDER BY `mqtt.simpleTest.Queued`.queuedObject
         * LIMIT 1;
         */

        println(createView(Queued::class, "queuedObject", "messageId", NonKeyedChild::class, "mqtt_inserted_id"))
        println()
        println(createView(Queued::class, "queuedObject", "messageId", PrimaryKeyChild::class, "identifier"))


        // Create triggers so if an item is
        println("\n\n/* Delete the objects from the queued table as a trigger */")
        val childTriggers = HashMap<KClass<*>, String>(2)
        childTriggers[NonKeyedChild::class] = "mqtt_inserted_id"
        childTriggers[PrimaryKeyChild::class] = "identifier"
        createChildMappingDeleteTrigger(Queued::class, "queuedObject", childTriggers).forEach {
            println(it)
        }
        // delete later from the queue
        println("\n\n/* Delete the queued objects */")
        println(delete("messageId", Queued::class.qualifiedName!!))
        println(delete("mqtt_inserted_id", NonKeyedChild::class.qualifiedName!!))
        println(delete("identifier", PrimaryKeyChild::class.qualifiedName!!))

        println("\n\n/* Inherited objects */")
        createTableInheritence<Vehicle>(CommercialTruck::class, Car::class).forEach { println(it) }


    }
}
