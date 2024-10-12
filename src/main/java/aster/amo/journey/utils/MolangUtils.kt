package aster.amo.journey.utils

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.data.JourneyDataObject
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.asMoLangValue
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
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
}
