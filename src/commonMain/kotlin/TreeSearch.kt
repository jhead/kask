package kask

import kotlinx.coroutines.yield
import kotlin.math.min

/**
 * Simple wrapper class for tree search instructions to be used by the individual implementations, such as DFS.
 *
 * @param adj     gets adjacent nodes from T
 * @param dedupe  optionally deduplicate nodes in the result, making them appear as late as possible
 */
data class TreeSearch<T>(val root: T, val adj: T.() -> Set<T>, val dedupe: Boolean = false)

/**
 * Tail-recursive DFS using immutable data structures.
 *
 * Example
 *
 *    __a__
 *   |     |
 *   b   __c__
 *      |     |
 *      b     d
 *
 * Returns: a, b, c, b, d
 * or with dedupe: a, c, b, d
 *
 * @param stack   the remaining tree to traverse, single root node to start
 * @param visited set of all visited vertices
 * @param prev    previously visited nodes, or null if at the root
 * @param acc     nodes accumulated so far, or an empty list to start
 * @return all nodes in the tree
 */
tailrec suspend fun <T> TreeSearch<T>.depthFirstSearch(
    stack: List<T> = listOf(root),
    visited: Set<Pair<T, T>> = emptySet(),
    prev: T? = null,
    acc: List<T> = emptyList()
): List<T> {
    val (node, stackTail) = stack.destructured

    // Stack is empty, we're done!
    if (node == null) {
        return acc
    }

    // Check for cycles by looking at whether we've already seen the current vertex (from -> to)
    val currentVertex = prev?.let { it to node } ?: (node to node)
    if (visited.contains(currentVertex)) {
        error("Detected cyclic dependency at ${currentVertex.first} -> ${currentVertex.second}")
    }

    // Add adjacent nodes to top of stack
    // todo: inefficient
    val newStack = node.adj().toList() + stackTail

    // Update acc with current node
    // If dedupe, removes the current node if it exists and places it at the end
    val newAcc = (if (dedupe) acc - node else acc) + node

    yield()

    // Recurse for next item in stack
    return depthFirstSearch(newStack, visited + currentVertex, node, newAcc)
}

private val <T> List<T>.tail: List<T> get() = subList(min(1, size), size)
private val <T> List<T>.head: T? get() = firstOrNull()
private val <T> List<T>.destructured get() = Pair(head, tail)
