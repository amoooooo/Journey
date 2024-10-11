package aster.amo.journey.utils.pathfinding
import aster.amo.journey.Journey
import aster.amo.journey.toBlockPos
import aster.amo.journey.utils.pathfinding.Pathfinder.reconstructPath
import com.github.shynixn.mccoroutine.fabric.scope
import kotlinx.coroutines.*
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.Blocks
import org.joml.Vector3i
import java.util.*
import kotlin.math.abs

class PathfinderService(private val server: MinecraftServer) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    val nodeComparator = compareBy<Node> { it.fScore }

    fun requestPath(
        start: Vector3i,
        goal: Vector3i,
        onPathFound: (List<Vector3i>) -> Unit,
        onPathNotFound: () -> Unit,
        level: ServerLevel
    ) {
        scope.launch {
            val path = findPathAsync(
                start = start,
                goal = goal,
                isPassable = ::isPassable,
                getMovementCost = ::getMovementCost,
                heuristic = ::heuristic,
                level = level
            )

            withContext(Journey.INSTANCE.scope.coroutineContext) { // Switch back to main thread for game interactions
                if (path != null) {
                    onPathFound(path)
                } else {
                    onPathNotFound()
                }
            }
        }
    }

    suspend fun findPathAsync(
        start: Vector3i,
        goal: Vector3i,
        isPassable: (Vector3i, ServerLevel) -> Boolean,
        getMovementCost: (Vector3i, Vector3i, ServerLevel) -> Double,
        heuristic: (Vector3i, Vector3i) -> Double,
        level: ServerLevel,
        maxIterations: Int = 10000
    ): List<Vector3i>? = withContext(Dispatchers.Default) { // Run on Default dispatcher
        val openSet = PriorityQueue(nodeComparator)
        val closedSet = mutableSetOf<Vector3i>()
        val allNodes = mutableMapOf<Vector3i, Node>()

        val startNode = Node(position = start, gScore = 0.0, fScore = heuristic(start, goal))
        openSet.add(startNode)
        allNodes[start] = startNode

        var iterations = 0

        Journey.LOGGER.debug("Starting pathfinding from $start to $goal")

        while (openSet.isNotEmpty() && iterations < maxIterations) {
            val currentNode = openSet.poll()
            Journey.LOGGER.debug("Processing node at ${currentNode.position} with fScore=${currentNode.fScore}")

            if (currentNode.position == goal) {
                Journey.LOGGER.debug("Goal reached at ${currentNode.position} after $iterations iterations")
                return@withContext reconstructPath(currentNode)
            }

            closedSet.add(currentNode.position)

            for (neighborPos in getNeighbors(currentNode.position)) {
                if (!isPassable(neighborPos, level)) {
                    Journey.LOGGER.debug("Neighbor $neighborPos is not passable")
                    continue
                }
                if (neighborPos in closedSet) {
                    Journey.LOGGER.debug("Neighbor $neighborPos is already in closed set")
                    continue
                }

                val tentativeGScore = currentNode.gScore + getMovementCost(currentNode.position, neighborPos, level)
                Journey.LOGGER.debug("Tentative gScore for $neighborPos is $tentativeGScore")

                val neighborNode = allNodes.getOrPut(neighborPos) { Node(position = neighborPos) }

                if (tentativeGScore < neighborNode.gScore) {
                    Journey.LOGGER.debug("Updating node at $neighborPos with new gScore=$tentativeGScore and fScore=${tentativeGScore + heuristic(neighborPos, goal)}")
                    neighborNode.cameFrom = currentNode
                    neighborNode.gScore = tentativeGScore
                    neighborNode.fScore = tentativeGScore + heuristic(neighborPos, goal)

                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode)
                        Journey.LOGGER.debug("Adding neighbor $neighborPos to open set")
                    } else {
                        // PriorityQueue doesn't automatically reorder, so remove and re-add
                        openSet.remove(neighborNode)
                        openSet.add(neighborNode)
                        Journey.LOGGER.debug("Re-inserting neighbor $neighborPos into open set")
                    }
                }
            }

            iterations++
        }

        if (iterations >= maxIterations) {
            Journey.LOGGER.warn("Pathfinding reached max iterations ($maxIterations) without finding a path")
        } else {
            Journey.LOGGER.warn("Pathfinding failed to find a path from $start to $goal")
        }

        // No path found
        null
    }

    fun isPassable(position: Vector3i, world: ServerLevel): Boolean {
        val block = world.getBlockState(position.toBlockPos()).block
        return when (block) {
            Blocks.AIR,
            Blocks.WATER -> true // Assuming water is passable
            else -> false
        }
    }

    // Determines if a block is dangerous
    fun isDangerous(position: Vector3i, world: ServerLevel): Boolean {
        val block = world.getBlockState(position.toBlockPos()).block
        return block == Blocks.FIRE || block == Blocks.LAVA
    }

    // Calculates danger level based on adjacent dangerous blocks
    fun getDangerLevel(position: Vector3i, world: ServerLevel): Int {
        val adjacentPositions = getNeighbors(position)
        var danger = 0
        for (pos in adjacentPositions) {
            if (isDangerous(pos, world)) {
                danger += 1
            }
        }
        return danger
    }

    // Calculates movement cost considering danger
    fun getMovementCost(currentPos: Vector3i, neighborPos: Vector3i, world: ServerLevel): Double {
        val baseCost = 1.0
        val dangerLevel = getDangerLevel(neighborPos, world)
        return baseCost + (dangerLevel * 2.0) // Adjust multiplier as needed
    }

    // Heuristic function (Manhattan distance)
    fun heuristic(pos: Vector3i, goalPos: Vector3i): Double {
        return (abs(pos.x - goalPos.x) + abs(pos.y - goalPos.y) + abs(pos.z - goalPos.z)).toDouble()
    }

    // Generates neighboring positions (6-directional)
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

    fun shutdown() {
        scope.cancel()
    }
}
