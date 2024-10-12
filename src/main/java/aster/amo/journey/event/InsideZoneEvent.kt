package aster.amo.journey.event

import aster.amo.journey.zones.Zone
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import xyz.nucleoid.stimuli.event.StimulusEvent

fun interface InsideZoneEvent {
    companion object {
        val EVENT: StimulusEvent<InsideZoneEvent> = StimulusEvent.create(InsideZoneEvent::class.java) { ctx ->
            return@create InsideZoneEvent { zone, player ->
                try {
                    for (listener in ctx.listeners) {
                        listener.onInsideZone(zone, player)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                InteractionResult.PASS
            }
        }
    }

    fun onInsideZone(zone: Zone, player: ServerPlayer)
}