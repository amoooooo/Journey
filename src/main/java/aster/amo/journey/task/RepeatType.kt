package aster.amo.journey.task

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.utils.parseToNative
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents

enum class RepeatType {
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    NONE
}

object RepeatHandler {
    fun setupReset(){
        ServerTickEvents.START_SERVER_TICK.register { server ->
            server.playerList.players.forEach { player ->
                val data = player get JourneyDataObject
                data.completedQuests.values.forEach { taskProgress ->
                    if(taskProgress.whenToReset != -1L && taskProgress.whenToReset <= System.currentTimeMillis()) {
                        taskProgress.reset()
                        data.rebuildSidebar()
                    }
                }
            }
        }
    }
    fun formatSecondsToTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return buildString {
            if (hours > 0) append("${hours}h")
            if (minutes > 0) append("${minutes}m")
            if (secs > 0 || (hours == 0 && minutes == 0)) append("${secs}s")
        }
    }
}