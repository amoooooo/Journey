package aster.amo.journey.event

import aster.amo.journey.zones.Zone
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import xyz.nucleoid.stimuli.event.StimulusEvent

fun interface EnterZoneEvent {
    companion object {
        val EVENT: StimulusEvent<EnterZoneEvent> = StimulusEvent.create(EnterZoneEvent::class.java) { ctx ->
            return@create EnterZoneEvent { zone, player ->
                try {
                    for (listener in ctx.listeners) {
                        listener.onEnterZone(zone, player)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                InteractionResult.PASS
            }
        }
    }

    fun onEnterZone(zone: Zone, player: ServerPlayer)
}