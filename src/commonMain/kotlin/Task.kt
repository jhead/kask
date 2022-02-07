package kask

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

data class Task(
    val name: String = generateTaskName(),
    val dependsOn: Set<TaskReference> = emptySet(),
    internal val taskImpl: suspend ScopedTaskContext.() -> Unit = { }
)

/** Executes the task on the current context and task executor */
suspend operator fun Task.invoke() = invoke(taskContext().executor)

suspend operator fun Task.invoke(executor: TaskExecutor) = executor.executeWithDependencies(this)

private fun generateTaskName(): String = randomString(16)

fun Task.dependsOn(vararg tasks: Task) = copy(dependsOn = dependsOn + tasks.map { it.ref }.toSet())
fun Task.dependsOn(task: TaskReference) = copy(dependsOn = dependsOn + task)

inline fun <reified T : Any> Task.outputAsync(
    key: PropertyKey<T>,
    executor: TaskExecutor = TaskExecutor.Default,
): Deferred<T> = CompletableDeferred<T>().also { defer ->
    val completionTask = Task {
        prop(key)?.let { output ->
            defer.complete(output)
        }
    }

    executor.onComplete(ref, completionTask)
}
