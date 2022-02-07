package kask

import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

typealias TaskCache = Map<TaskReference, CompletedTask>

/** Context provided to all tasks during execution */
interface TaskContext : PropertySet {
    val executor: TaskExecutor
    val taskCache: TaskCache
    val properties: PropertySet
}

/** Immutable task context stored in the coroutine context, enabling tasks to safely share a common context */
internal data class SharedTaskContext(
    override val executor: TaskExecutor,
    override val taskCache: TaskCache = emptyMap(),
    override val properties: PropertySet = EmptyPropertySet,
) : TaskContext, PropertySet by properties, CoroutineContext.Element {

    object Key : CoroutineContext.Key<SharedTaskContext>
    override val key get() = Key

}

/** Retrieves the shared task context from the current coroutine context, if available */
internal suspend inline fun taskContextOrNull(): SharedTaskContext? =
    coroutineContext[SharedTaskContext.Key]

internal suspend inline fun taskContext(): SharedTaskContext =
    taskContextOrNull() ?: error("Not called from a TaskContext")

suspend fun <T> withTaskContext(block: suspend TaskContext.() -> T): T =
    with (taskContext()) {
        withContext(this) {
            block()
        }
    }

/** Task context scoped to a single task and execution with scoped mutability */
class ScopedTaskContext(
    val task: Task,
    override val executor: TaskExecutor,
    override val taskCache: TaskCache,
    override val properties: MutableMap<PropertyKey<*>, Property<*>>,
) : TaskContext, PropertySet by properties

internal fun TaskContext.scoped(task: Task) = ScopedTaskContext(
    task,
    executor,
    taskCache,
    toMutableMap(),
)

internal val TaskContext.shared get() = when (this) {
    is SharedTaskContext -> this
    else -> SharedTaskContext(
        executor,
        taskCache,
        toMap()
    )
}
