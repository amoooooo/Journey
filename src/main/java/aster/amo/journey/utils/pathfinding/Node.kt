package aster.amo.journey.utils.pathfinding

import org.joml.Vector3i

data class Node(
    val position: Vector3i,
    var gScore: Double = Double.MAX_VALUE,
    var fScore: Double = Double.MAX_VALUE,
    var cameFrom: Node? = null
)