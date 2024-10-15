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
        val initialZones = ConfigManager.ZONE_CONFIG.zones.toList()
        initialZones.forEach { zone ->
            zones.add(zone)
        }

        zones.subscribe {
            updateZoneConfig()
        }
    }

    /**
     * Updates the ConfigManager.ZONE_CONFIG.zones with the current zones.
     * This function ensures that the update is done safely without causing concurrent modifications.
     */
    private fun updateZoneConfig() {
        ConfigManager.ZONE_CONFIG.zones.clear()
        ConfigManager.ZONE_CONFIG.zones.addAll(zones)
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
