package aster.amo.journey.event

import aster.amo.journey.zones.Zone
import aster.amo.journey.zones.area.ZoneArea
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import xyz.nucleoid.stimuli.event.StimulusEvent

fun interface EnterZoneAreaEvent {
    companion object {
        val EVENT: StimulusEvent<EnterZoneAreaEvent> = StimulusEvent.create(EnterZoneAreaEvent::class.java) { ctx ->
            return@create EnterZoneAreaEvent { zone, area, player ->
                try {
                    for (listener in ctx.listeners) {
                        listener.onEnterZone(zone, area, player)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                InteractionResult.PASS
            }
        }
    }

    fun onEnterZone(zone: Zone, area: ZoneArea, player: ServerPlayer)
}