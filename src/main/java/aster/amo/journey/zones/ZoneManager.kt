package aster.amo.journey.zones

import com.cobblemon.mod.common.util.subscribeOnServer
import com.cobblemon.mod.common.util.toVec3d
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import aster.amo.journey.config.ConfigManager
import aster.amo.journey.reactive.collections.list.MutableObservableList
import java.util.UUID

class ZoneManager(
    val zones: MutableObservableList<Zone> = MutableObservableList(
        list = mutableListOf(
        )
    )
) {

    fun init() {
        ConfigManager.ZONE_CONFIG.zones.forEach { zones.add(it) }
        zones.subscribe {
            ConfigManager.ZONE_CONFIG.zones.clear()
            ConfigManager.ZONE_CONFIG.zones.addAll(zones)
            ConfigManager.saveFile("zones.json", ConfigManager.ZONE_CONFIG)
        }
    }

    fun addZone(zone: Zone) {
        zones.add(zone)
    }

    fun removeZone(zone: Zone) {
        zones.remove(zone)
    }

    fun findZoneContaining(pos: BlockPos): Zone? {
        return zones.firstOrNull { it.findAreaContaining(pos) != null }
    }

    fun onTick(server: MinecraftServer) {
        zones.forEach { it.onTick(server) }
    }
}
