package aster.amo.journey.zones.area

import aster.amo.journey.Journey
import aster.amo.journey.event.EnterZoneAreaEvent
import com.cobblemon.mod.common.util.asResource
import com.google.gson.annotations.SerializedName
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageTypes
import net.minecraft.world.level.Level
import org.joml.Vector3f
import aster.amo.journey.utils.ConfigTitle
import com.bedrockk.molang.runtime.struct.QueryStruct
import xyz.nucleoid.stimuli.Stimuli
import xyz.nucleoid.stimuli.event.EventListenerMap
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent
import xyz.nucleoid.stimuli.event.player.PlayerConsumeHungerEvent
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent
import xyz.nucleoid.stimuli.filter.EventFilter
import xyz.nucleoid.stimuli.selector.SimpleListenerSelector
import java.util.*

abstract class ZoneArea(
    @SerializedName("dimension") val dimension: String,
    @SerializedName("entry_message") val entryMessage: ConfigTitle = ConfigTitle(),
    @SerializedName("exit_message") val exitMessage: ConfigTitle = ConfigTitle(),
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("functions") val functions: List<ZoneFunction> = emptyList()
) {
    val playersInside = mutableSetOf<UUID>()
    var dimensionKey: ResourceKey<Level> = ResourceKey.create(Registries.DIMENSION, dimension.asResource())
    abstract fun contains(pos: BlockPos): Boolean
    abstract fun onTick(server: MinecraftServer)
    abstract fun drawOutline(level: ServerLevel, particle: ParticleOptions, particlesPerBlock: Double, color: Vector3f)
    abstract fun getStimuliZone(): EventFilter

    open fun onPlayerEnter(player: ServerPlayer) {
        if (entryMessage != null) {
            entryMessage.type.run(player, entryMessage)
        }
    }

    open fun onPlayerInside(player: ServerPlayer) {
        val parentZone = Journey.INSTANCE.zoneManager.zones.firstOrNull { it.areas.contains(this) }
        if (parentZone != null) {
            Stimuli.select().forEntity(player).get(EnterZoneAreaEvent.EVENT).onEnterZone(parentZone, this, player)
        }
    }
    open fun onPlayerLeave(player: ServerPlayer) {
        if (exitMessage != null) {
            exitMessage.type.run(player, exitMessage)
        }
    }

    abstract fun toStruct(): QueryStruct

    enum class ZoneFunction(val function: (ZoneArea) -> Unit) {
        NONE({ _ -> }),
        DENY_FALL_DAMAGE({ area->
            val areaFilter = area.getStimuliZone()
            val listeners = EventListenerMap()
            listeners.listen(PlayerDamageEvent.EVENT, PlayerDamageEvent { player, source, amount ->
                if (source.`is`(DamageTypes.FALL)) {
                    return@PlayerDamageEvent InteractionResult.FAIL
                }
                return@PlayerDamageEvent InteractionResult.PASS
            })
            Stimuli.registerSelector(SimpleListenerSelector(areaFilter, listeners))
        }),
        DENY_ALL_DAMAGE({ area ->
            val areaFilter = area.getStimuliZone()
            val listeners = EventListenerMap()
            listeners.listen(PlayerDamageEvent.EVENT, PlayerDamageEvent { player, source, amount ->
                return@PlayerDamageEvent InteractionResult.FAIL
            })
            Stimuli.registerSelector(SimpleListenerSelector(areaFilter, listeners))
        }),
        DENY_HUNGER({ area ->
            val areaFilter = area.getStimuliZone()
            val listeners = EventListenerMap()
            listeners.listen(
                PlayerConsumeHungerEvent.EVENT,
                PlayerConsumeHungerEvent { player, foodLevel, saturation, exhaustion ->
                    return@PlayerConsumeHungerEvent InteractionResult.PASS
                })
            Stimuli.registerSelector(SimpleListenerSelector(areaFilter, listeners))
        }),
        DENY_ATTACK({ area ->
            val areaFilter = area.getStimuliZone()
            val listeners = EventListenerMap()
            listeners.listen(
                PlayerAttackEntityEvent.EVENT,
                PlayerAttackEntityEvent { attacker, hand, attacked, hitResult ->
                    return@PlayerAttackEntityEvent InteractionResult.PASS
                })
            Stimuli.registerSelector(SimpleListenerSelector(areaFilter, listeners))
        })
    }
}