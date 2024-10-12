package aster.amo.journey.event

import aster.amo.journey.zones.Zone
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import xyz.nucleoid.stimuli.event.StimulusEvent

fun interface ExitZoneEvent {
    companion object {
        val EVENT: StimulusEvent<ExitZoneEvent> = StimulusEvent.create(ExitZoneEvent::class.java) { ctx ->
            return@create ExitZoneEvent { zone, player ->
                try {
                    for (listener in ctx.listeners) {
                        listener.onExitZone(zone, player)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                InteractionResult.PASS
            }
        }
    }

    fun onExitZone(zone: Zone, player: ServerPlayer)
}