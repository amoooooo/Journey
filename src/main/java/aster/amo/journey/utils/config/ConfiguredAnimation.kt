package aster.amo.journey.utils.config

class ConfiguredAnimation(
    val animation: String = "",
    val repeat: RepeatType = RepeatType.NONE,
    val interval: Int = 0
) {
    enum class RepeatType {
        NONE,
        LOOP
    }
}