package kask

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

private class TaskTest {

    object TestOutput : PropertyKey<String>()

    @Test
    fun test(): Unit = runBlocking {
        val first = Task(name = "first") {
            println("first task!")

            properties[TestOutput] = Property("test")
        }

        val second = Task {
            println("second task!")

            // Properties from first task are automatically available due to shared context
            assertEquals("test", prop(TestOutput))
        }.dependsOn(first)

        // Register tasks to be executed
        TaskRegistry.Default.register(first, second)

        // Run second task (and first)
        second(TaskExecutor.Default)

        // Properties can be retrieved once a task is complete
        val propFromFirst = first.outputAsync(TestOutput).await()
        assertEquals("test", propFromFirst)
    }

}
