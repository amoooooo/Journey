package aster.amo.journey.utils

import org.joml.Vector3f

/**
 * GSON-serializable color class with hex string representation and random color generation.
 */
class Color(val red: Int, val green: Int, val blue: Int) {
    fun toInt(): Int {
        return (red shl 16) or (green shl 8) or blue
    }

    fun toVector3f(): Vector3f {
        return Vector3f(red / 255f, green / 255f, blue / 255f)
    }

    constructor(hex: String) : this(
        hex.substring(1, 3).toInt(16),
        hex.substring(3, 5).toInt(16),
        hex.substring(5, 7).toInt(16)
    )

    val hex: String
        get() = "#${red.toString(16).padStart(2, '0')}${green.toString(16).padStart(2, '0')}${blue.toString(16).padStart(2, '0')}"

    companion object {
        fun random(): Color {
            return Color((0..255).random(), (0..255).random(), (0..255).random())
        }
    }
}
