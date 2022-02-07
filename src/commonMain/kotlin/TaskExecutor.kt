package kask

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface TaskExecutor {

    object Default : AbstractTaskExecutor(TaskRegistry.Default)

    suspend fun executeWithDependencies(task: Task)

    fun onComplete(task: TaskReference, onCompleteTask: Task)

}

abstract class AbstractTaskExecutor(
    private val registry: TaskRegistry,
) : TaskExecutor {

    private val observer = TaskObserver()

    override suspend fun executeWithDependencies(task: Task) {
        val deps = task.dependencies()
        (deps.toList() + task).executeAll()
    }

    private suspend fun Collection<Task>.executeAll(): TaskContext =
        fold(taskContextOrNew()) { ctx, task ->
            withContext(ctx) {
                val scoped = ctx.scoped(task)
                task.executeOnce(scoped)

                val nextContext = scoped.shared.let {
                    // TODO gross
                    it.copy(
                        taskCache = it.taskCache + (task.ref to CompletedTask(task.ref, scoped.shared))
                    )
                }

                nextContext
            }
        }

    private suspend fun Task.executeOnce(ctx: ScopedTaskContext) {
        val hasExecuted = ctx.taskCache.contains(ref)
        if (!hasExecuted) {
            taskImpl(ctx)

            observer.observe(CompletedTask(ref, ctx.shared))
        } else {
            println("Task '$name' cached, skipping")
        }
    }

    private suspend fun taskContextOrNew(): SharedTaskContext =
        taskContextOrNull() ?: SharedTaskContext(this)

    private suspend fun Task.dependencies(): HashSet<Task> =
        TreeSearch(this, { dependsOn.map { it.task }.toSet() }, dedupe = true)
            .depthFirstSearch()
            .asReversed()
            .let { it.subList(0, it.size - 1) }
            .toHashSet()

    private val TaskReference.task get() = registry[this] ?: error("Task '$taskName' not found")

    override fun onComplete(task: TaskReference, onCompleteTask: Task) {
        CoroutineScope(Dispatchers.Unconfined).launch {
            val (_, ctx) = observer.tasks.first { it.task == task }
            withContext(ctx.shared) {
                onCompleteTask(this@AbstractTaskExecutor)
            }
        }
    }

}

private class TaskObserver {

    private val _taskVisitorChannel = MutableSharedFlow<CompletedTask>(
        replay = Int.MAX_VALUE,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val tasks: Flow<CompletedTask> get() = _taskVisitorChannel.asSharedFlow()

    suspend fun observe(task: CompletedTask) {
        _taskVisitorChannel.emit(task)
    }

}

data class CompletedTask(val task: TaskReference, val ctx: TaskContext) {
    companion object {
        suspend operator fun invoke(task: TaskReference) =
            withTaskContext { CompletedTask(task, this) }
    }
}
