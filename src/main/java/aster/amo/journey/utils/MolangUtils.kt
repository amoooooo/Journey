package aster.amo.journey.utils

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.data.JourneyDataObject
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.molang.MoLangFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import com.cobblemon.mod.common.entity.npc.NPCEntity
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack

object MolangUtils {
    fun setupPlayerStructs(queryStruct: QueryStruct, serverPlayer: ServerPlayer) {
        // Add 'player' function to 'query' struct
        queryStruct.addFunctions(
            mapOf(
                "player" to java.util.function.Function { params ->
                    return@Function serverPlayer.asMoLangValue().addFunctions(hashMapOf(
                        // Add functions within 'player' struct

                    ))
                }
            )
        )
    }

    fun setupWorldStructs(queryStruct: QueryStruct, serverPlayer: ServerPlayer) {
        queryStruct.addFunctions(MoLangFunctions.worldFunctions)
    }

    fun setupNPCStructs(queryStruct: QueryStruct, entity: NPCEntity) {
        // Add 'npc' function to 'query' struct
        queryStruct.addFunctions(
            mapOf(
                "npc" to java.util.function.Function { params ->
                    return@Function entity.asMoLangValue().addFunctions(hashMapOf(
                        // Add functions within 'npc' struct

                    ))
                }
            )
        )
    }

    fun setupEntityStructs(queryStruct: QueryStruct, entity: LivingEntity) {
        // Add 'entity' function to 'query' struct
        queryStruct.addFunctions(
            MoLangFunctions.entityFunctions.flatMap { it(entity).entries.map { it.key to it.value } }.toMap()
        )
    }
}
