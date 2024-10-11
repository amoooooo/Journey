package aster.amo.journey.utils.pathfinding
import kotlinx.coroutines.*
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3i
import java.util.*

object Pathfinder {
    // Comparator for PriorityQueue based on fScore
    val nodeComparator = compareBy<Node> { it.fScore }

    // A* Pathfinding function
    suspend fun findPath(
        start: Vector3i,
        goal: Vector3i,
        isPassable: (Vector3i) -> Boolean,
        getMovementCost: (Vector3i, Vector3i) -> Double,
        heuristic: (Vector3i, Vector3i) -> Double,
        maxIterations: Int = 10000 // Prevent infinite loops
    ): List<Vector3i>? = coroutineScope {
        val openSet = PriorityQueue(nodeComparator)
        val closedSet = mutableSetOf<Vector3i>()
        val allNodes = mutableMapOf<Vector3i, Node>()

        val startNode = Node(position = start, gScore = 0.0, fScore = heuristic(start, goal))
        openSet.add(startNode)
        allNodes[start] = startNode

        var iterations = 0

        while (openSet.isNotEmpty() && iterations < maxIterations) {
            val currentNode = openSet.poll()

            if (currentNode.position == goal) {
                // Reconstruct and return the path
                return@coroutineScope reconstructPath(currentNode)
            }

            closedSet.add(currentNode.position)

            for (neighborPos in getNeighbors(currentNode.position)) {
                if (!isPassable(neighborPos) || neighborPos in closedSet) {
                    continue
                }

                val tentativeGScore = currentNode.gScore + getMovementCost(currentNode.position, neighborPos)

                val neighborNode = allNodes.getOrPut(neighborPos) { Node(position = neighborPos) }

                if (tentativeGScore < neighborNode.gScore) {
                    neighborNode.cameFrom = currentNode
                    neighborNode.gScore = tentativeGScore
                    neighborNode.fScore = tentativeGScore + heuristic(neighborPos, goal)

                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode)
                    } else {
                        // Since PriorityQueue doesn't update the position, we need to remove and re-add
                        openSet.remove(neighborNode)
                        openSet.add(neighborNode)
                    }
                }
            }

            iterations++
            // Yield control to prevent blocking
            yield()
        }

        // If max iterations reached without finding a path
        null
    }
    fun getNeighbors(position: Vector3i): List<Vector3i> {
        val directions = listOf(
            Vector3i(1, 0, 0),
            Vector3i(-1, 0, 0),
            Vector3i(0, 1, 0),
            Vector3i(0, -1, 0),
            Vector3i(0, 0, 1),
            Vector3i(0, 0, -1)
        )
        return directions.map { dir ->
            Vector3i(position.x + dir.x, position.y + dir.y, position.z + dir.z)
        }
    }

    fun reconstructPath(node: Node): List<Vector3i> {
        val path = mutableListOf<Vector3i>()
        var currentNode: Node? = node
        while (currentNode != null) {
            path.add(currentNode.position)
            currentNode = currentNode.cameFrom
        }
        return path.reversed()
    }

    fun getMovementCost(currentPos: Vector3i, neighborPos: Vector3i,level: ServerLevel): Double {
        val baseCost = 1.0
        val dangerPenalty = getDangerLevel(neighborPos, level) * 5.0 // Adjust the multiplier as needed
        return baseCost + dangerPenalty
    }

    fun getDangerLevel(position: Vector3i, level: ServerLevel): Int {
        var dangerLevel = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    val checkPos = Vector3i(position.x + dx, position.y + dy, position.z + dz)
                    if (isDangerousBlock(checkPos, level)) {
                        dangerLevel += 1
                    }
                }
            }
        }
        return dangerLevel
    }

    fun isDangerousBlock(position: Vector3i, level: ServerLevel): Boolean {
        val blockState = level.getBlockState(BlockPos(position.x, position.y, position.z))
        return !blockState.isAir || blockState.isSuffocating(level, BlockPos(position.x, position.y, position.z)) || !blockState.fluidState.isEmpty
    }
}