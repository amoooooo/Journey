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
                        "has_item" to java.util.function.Function { params ->
                            val itemId = params.getString(0)
                            val hasItem = serverPlayer.inventory.contains(
                                ItemStack(
                                    BuiltInRegistries.ITEM.get(
                                        ResourceLocation.parse(itemId)
                                    )
                                )
                            )
                            DoubleValue(if (hasItem) 1.0 else 0.0)
                        },
                        "pokedex" to java.util.function.Function { params ->
                            // Return Pokédex data struct
                            Cobblemon.playerDataManager.getPokedexData(serverPlayer).struct
                        },
                        "starter_pokemon" to java.util.function.Function { params ->
                            val data = serverPlayer get JourneyDataObject
                            data.starterPokemon?.let {
                                Cobblemon.storage.getParty(serverPlayer)[it]
                            } ?: QueryStruct(hashMapOf())
                        }
                    ))
                }
            )
        )
    }
}
