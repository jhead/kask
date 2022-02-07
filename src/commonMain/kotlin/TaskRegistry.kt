package kask

interface TaskRegistry : Map<TaskReference, Task> {

    fun register(tasks: Collection<Task>)

    object Default : TaskRegistry, MutableMap<TaskReference, Task> by mutableMapOf() {
        override fun register(tasks: Collection<Task>) = putAll(tasks.map { it.ref to it })
    }

}

fun TaskRegistry.register(vararg tasks: Task) = register(tasks.toSet())

data class TaskReference(val taskName: String)

val Task.ref: TaskReference get() = TaskReference(name)
