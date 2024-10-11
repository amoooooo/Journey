package aster.amo.journey.task.event

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.data.JourneyDataObject
import aster.amo.journey.utils.MolangUtils
import aster.amo.journey.utils.registryName
import com.bedrockk.molang.runtime.MoLangRuntime
import com.bedrockk.molang.runtime.struct.QueryStruct
import com.bedrockk.molang.runtime.value.DoubleValue
import com.bedrockk.molang.runtime.value.StringValue
import com.cobblemon.mod.common.api.battles.model.actor.ActorType
import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.molang.MoLangFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.addFunctions
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor
import com.cobblemon.mod.common.util.resolveBoolean
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import xyz.nucleoid.stimuli.Stimuli
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent
import xyz.nucleoid.stimuli.event.item.ItemPickupEvent

object JourneyEvents {

    val ENTITY_INTERACT = JourneyEvent("ENTITY_INTERACT") {
        Stimuli.global()
            .listen(EntityUseEvent.EVENT, EntityUseEvent { serverPlayer, entity, interactionHand, entityHitResult ->
                if (entity !is LivingEntity) return@EntityUseEvent InteractionResult.PASS
                val data = serverPlayer get JourneyDataObject
                val activeSubtasks = data.getActiveSubtasks("ENTITY_INTERACT")
                activeSubtasks.forEach { (taskId, subtask, progress) ->
                    val runtime = createMolangRuntime()
                    val queryStruct = runtime.environment.getStruct("query") as QueryStruct
                    MolangUtils.setupPlayerStructs(queryStruct, serverPlayer)
                    queryStruct.addFunctions(
                        mapOf(
                            "entity" to java.util.function.Function { params ->
                                return@Function QueryStruct(MoLangFunctions.entityFunctions
                                    .flatMap { function ->
                                        function.invoke(entity).entries
                                    }
                                    .associate { entry ->
                                        entry.key to entry.value
                                    }.apply {
                                        this.plus(Pair("uuid", java.util.function.Function { StringValue(entity.uuid.toString()) }))
                                    } as HashMap)
                            }
                        )

                    )
                    queryStruct.addFunctions(
                        mapOf(
                            "event" to java.util.function.Function { params ->
                                return@Function QueryStruct(hashMapOf(
                                    "hand" to java.util.function.Function { params ->
                                        StringValue(interactionHand.name)
                                    }
                                ))
                            }
                        )
                    )
                    val filterExpression = subtask.getOrParseFilterExpression()
                    val result = runtime.resolveBoolean(filterExpression)
                    if (result) {
                        progress.progress += 1.0
                        if (progress.progress >= subtask.target) {
                            subtask.rewards.ifNotEmpty { it.forEach { it.parse(serverPlayer) } }
                            data.activeSubtasksByEvent[subtask.event.name]?.remove(progress)
                            data.checkAndCompleteTask(taskId, subtask, progress)
                        }
                    }
                }
                InteractionResult.PASS
            })
    }


    val BATTLE_VICTORY = JourneyEvent("BATTLE_VICTORY") {
        CobblemonEvents.BATTLE_VICTORY.subscribe { event ->
            event.winners.forEach { winner ->
                if (winner is PlayerBattleActor) {
                    val player = winner.entity ?: return@forEach
                    val data = player get JourneyDataObject
                    val activeSubtasks = data.getActiveSubtasks("BATTLE_VICTORY")
                    activeSubtasks.forEach { (taskId, subtask, progress) ->
                        val runtime = createMolangRuntime()
                        val queryStruct = runtime.environment.getStruct("query") as QueryStruct
                        MolangUtils.setupPlayerStructs(queryStruct, player)

                        // Add 'battle' function to 'query' struct
                        queryStruct.addFunctions(
                            mapOf(
                                "battle" to java.util.function.Function { params ->
                                    // Create 'battle' struct with functions
                                    return@Function QueryStruct(hashMapOf(
                                        "is_wild" to java.util.function.Function { params ->
                                            DoubleValue(if (event.battle.isPvW) 1.0 else 0.0)
                                        },
                                        "opponent" to java.util.function.Function { params ->
                                            val opponentActor = event.battle.actors.find { it.type == ActorType.WILD }
                                            if (opponentActor != null) {
                                                val opponentPokemon =
                                                    opponentActor.pokemonList[params.getDouble(0).toInt()]
                                                if (opponentPokemon != null) {
                                                    return@Function QueryStruct(hashMapOf(
                                                        "species" to java.util.function.Function { params ->
                                                            StringValue(opponentPokemon.originalPokemon.species.resourceIdentifier.toString())
                                                        },
                                                        "form" to java.util.function.Function { params ->
                                                            StringValue(opponentPokemon.originalPokemon.form.name)
                                                        },
                                                        "level" to java.util.function.Function { params ->
                                                            DoubleValue(opponentPokemon.originalPokemon.level.toDouble())
                                                        }
                                                        // Add other opponent fields if needed
                                                    ))
                                                }
                                            }
                                            return@Function QueryStruct(hashMapOf())
                                        },
                                        "team" to java.util.function.Function { params ->
                                            val pokemonList = event.battle.actors.filterIsInstance<PlayerBattleActor>()
                                                .first().pokemonList
                                            return@Function QueryStruct(hashMapOf(
                                                "pokemon" to java.util.function.Function { params ->
                                                    val index = params.getDouble(0).toInt()
                                                    val pokemon = pokemonList.getOrNull(index)
                                                    if (pokemon != null) {
                                                        return@Function QueryStruct(hashMapOf(
                                                            "species" to java.util.function.Function { params ->
                                                                StringValue(pokemon.originalPokemon.species.resourceIdentifier.toString())
                                                            },
                                                            "form" to java.util.function.Function { params ->
                                                                StringValue(pokemon.originalPokemon.form.name)
                                                            },
                                                            "level" to java.util.function.Function { params ->
                                                                DoubleValue(pokemon.originalPokemon.level.toDouble())
                                                            },
                                                            "primary_type" to java.util.function.Function { params ->
                                                                StringValue(pokemon.originalPokemon.types.first().name)
                                                            }
                                                            // Add other fields if needed
                                                        ))
                                                    }
                                                    return@Function QueryStruct(hashMapOf())
                                                },
                                                "contains_starter" to java.util.function.Function { params ->
                                                    val data = player get JourneyDataObject
                                                    val starterPokemon = data.starterPokemon
                                                    DoubleValue(if (starterPokemon != null && pokemonList.any { it.uuid == starterPokemon }) 1.0 else 0.0)
                                                }
                                            ))
                                        }
                                    ))
                                }
                            )
                        )

                        val filterExpression = subtask.getOrParseFilterExpression()
                        val result = runtime.resolveBoolean(filterExpression)
                        if (result) {
                            progress.progress += 1.0
                            if (progress.progress >= subtask.target) {
                                subtask.rewards.ifNotEmpty { it.forEach { it.parse(player) } }
                                data.activeSubtasksByEvent[subtask.event.name]?.remove(progress)
                                data.checkAndCompleteTask(taskId, subtask, progress)
                            }
                        }
                    }
                }
            }
        }
    }

    val BATTLE_FLED = JourneyEvent("BATTLE_FLED") {
        CobblemonEvents.BATTLE_FLED.subscribe { event ->
            val player = event.player.entity ?: return@subscribe
            val data = player get JourneyDataObject
            val activeSubtasks = data.getActiveSubtasks("BATTLE_FLED")
            activeSubtasks.forEach { (taskId, subtask, progress) ->
                val runtime = createMolangRuntime()
                val queryStruct = runtime.environment.getStruct("query") as QueryStruct
                MolangUtils.setupPlayerStructs(queryStruct, player)

                // Add 'battle' function to 'query' struct
                queryStruct.addFunctions(
                    mapOf(
                        "battle" to java.util.function.Function { params ->
                            // Create 'battle' struct with functions
                            return@Function QueryStruct(hashMapOf(
                                "isPvW" to java.util.function.Function { params ->
                                    DoubleValue(if (event.battle.isPvW) 1.0 else 0.0)
                                },
                                "opponent" to java.util.function.Function { params ->
                                    val opponentActor = event.battle.actors.find { it.type == ActorType.WILD }
                                    if (opponentActor != null) {
                                        val opponentPokemon = opponentActor.pokemonList.firstOrNull()
                                        if (opponentPokemon != null) {
                                            return@Function QueryStruct(hashMapOf(
                                                "species" to java.util.function.Function { params ->
                                                    StringValue(opponentPokemon.originalPokemon.species.resourceIdentifier.toString())
                                                },
                                                "form" to java.util.function.Function { params ->
                                                    StringValue(opponentPokemon.originalPokemon.form.name)
                                                },
                                                "level" to java.util.function.Function { params ->
                                                    DoubleValue(opponentPokemon.originalPokemon.level.toDouble())
                                                },
                                                "primary_type" to java.util.function.Function { params ->
                                                    StringValue(opponentPokemon.originalPokemon.types.first().name)
                                                }
                                                // Add other opponent fields if needed
                                            ))
                                        }
                                    }
                                    return@Function QueryStruct(hashMapOf())
                                }
                            ))
                        }
                    )
                )

                val filterExpression = subtask.getOrParseFilterExpression()
                val result = runtime.resolveBoolean(filterExpression)
                if (result) {
                    progress.progress += 1.0
                    if (progress.progress >= subtask.target) {
                        subtask.rewards.ifNotEmpty { it.forEach { it.parse(player) } }
                        data.activeSubtasksByEvent[subtask.event.name]?.remove(progress)
                        data.checkAndCompleteTask(taskId, subtask, progress)
                    }
                }
            }
        }
    }

    val ITEM_PICKUP = JourneyEvent("ITEM_PICKUP") {
        Stimuli.global().listen(ItemPickupEvent.EVENT, ItemPickupEvent { serverPlayer, itemEntity, itemStack ->
            val data = serverPlayer get JourneyDataObject
            val activeSubtasks = data.getActiveSubtasks("ITEM_PICKUP")

            activeSubtasks.forEach { (taskId, subtask, progress) ->
                val runtime = createMolangRuntime()
                val queryStruct = runtime.environment.getStruct("query") as QueryStruct
                MolangUtils.setupPlayerStructs(queryStruct, serverPlayer)

                // Add 'item' function to 'query' struct
                queryStruct.addFunctions(
                    mapOf(
                        "item" to java.util.function.Function { params ->
                            return@Function QueryStruct(hashMapOf(
                                "id" to java.util.function.Function { params ->
                                    StringValue(itemEntity.item.registryName().toString())
                                },
                                "count" to java.util.function.Function { params ->
                                    DoubleValue(itemEntity.item.count.toDouble())
                                }
                            ))
                        }
                    )
                )

                val filterExpression = subtask.getOrParseFilterExpression()
                val result = runtime.resolveBoolean(filterExpression)
                if (result) {
                    progress.progress += itemEntity.item.count.toDouble()
                    if (progress.progress >= subtask.target) {
                        subtask.rewards.ifNotEmpty { it.forEach { it.parse(serverPlayer) } }
                        data.activeSubtasksByEvent[subtask.event.name]?.remove(progress)
                        data.checkAndCompleteTask(taskId, subtask, progress)
                    }
                }
            }
            InteractionResult.PASS
        })
    }

    val POKEMON_CAPTURE = JourneyEvent("POKEMON_CAUGHT") {
        CobblemonEvents.POKEMON_CAPTURED.subscribe { event ->
            val player = event.player
            val data = player get JourneyDataObject
            val activeSubtasks = data.getActiveSubtasks("POKEMON_CAPTURE")
            activeSubtasks.forEach { (taskId, subtask, progress) ->
                val runtime = createMolangRuntime()
                val queryStruct = runtime.environment.getStruct("query") as QueryStruct
                MolangUtils.setupPlayerStructs(queryStruct, player)

                // Add 'pokemon' function to 'query' struct
                queryStruct.addFunctions(
                    mapOf(
                        "pokemon" to java.util.function.Function { params ->
                            return@Function QueryStruct(hashMapOf(
                                "species" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.species.resourceIdentifier.toString())
                                },
                                "form" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.form.name)
                                },
                                "level" to java.util.function.Function { params ->
                                    DoubleValue(event.pokemon.level.toDouble())
                                },
                                "is_starter" to java.util.function.Function { params ->
                                    val data = player get JourneyDataObject
                                    val starterPokemon = data.starterPokemon
                                    DoubleValue(if (starterPokemon != null && starterPokemon == event.pokemon.uuid) 1.0 else 0.0)
                                },
                                "primary_type" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.types.first().name)
                                }
                                // Add other fields if needed
                            ))
                        }
                    )
                )

                val filterExpression = subtask.getOrParseFilterExpression()
                val result = runtime.resolveBoolean(filterExpression)
                if (result) {
                    progress.progress += 1.0
                    if (progress.progress >= subtask.target) {
                        subtask.rewards.ifNotEmpty { it.forEach { it.parse(player) } }
                        data.activeSubtasksByEvent[subtask.event.name]?.remove(progress)
                        data.checkAndCompleteTask(taskId, subtask, progress)
                    }
                }
            }
        }
    }

    val POKEMON_EVOLVE = JourneyEvent("POKEMON_EVOLVE") {
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe { event ->
            val player = event.pokemon.getOwnerPlayer() ?: return@subscribe
            val data = player get JourneyDataObject
            val activeSubtasks = data.getActiveSubtasks("POKEMON_EVOLVE")
            activeSubtasks.forEach { (taskId, subtask, progress) ->
                val runtime = createMolangRuntime()
                val queryStruct = runtime.environment.getStruct("query") as QueryStruct
                MolangUtils.setupPlayerStructs(queryStruct, player)

                // Add 'pokemon' function to 'query' struct
                queryStruct.addFunctions(
                    mapOf(
                        "pokemon" to java.util.function.Function { params ->
                            return@Function QueryStruct(hashMapOf(
                                "species" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.species.resourceIdentifier.toString())
                                },
                                "form" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.form.name)
                                },
                                "level" to java.util.function.Function { params ->
                                    DoubleValue(event.pokemon.level.toDouble())
                                },
                                "is_starter" to java.util.function.Function { params ->
                                    val data = player get JourneyDataObject
                                    val starterPokemon = data.starterPokemon
                                    DoubleValue(if (starterPokemon != null && starterPokemon == event.pokemon.uuid) 1.0 else 0.0)
                                },
                                "primary_type" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.types.first().name)
                                }
                                // Add other fields if needed
                            ))
                        }
                    )
                )

                val filterExpression = subtask.getOrParseFilterExpression()
                val result = runtime.resolveBoolean(filterExpression)
                if (result) {
                    progress.progress += 1.0
                    if (progress.progress >= subtask.target) {
                        subtask.rewards.ifNotEmpty { it.forEach { it.parse(player) } }
                        data.activeSubtasksByEvent[subtask.event.name]?.remove(progress)
                        data.checkAndCompleteTask(taskId, subtask, progress)
                    }
                }
            }
        }
    }

    val POKEMON_LEVEL_UP = JourneyEvent("POKEMON_LEVEL_UP") {
        CobblemonEvents.LEVEL_UP_EVENT.subscribe { event ->
            val player = event.pokemon.getOwnerPlayer() ?: return@subscribe
            val data = player get JourneyDataObject
            val activeSubtasks = data.getActiveSubtasks("POKEMON_LEVEL_UP")
            activeSubtasks.forEach { (taskId, subtask, progress) ->
                val runtime = createMolangRuntime()
                val queryStruct = runtime.environment.getStruct("query") as QueryStruct
                MolangUtils.setupPlayerStructs(queryStruct, player)

                // Add 'pokemon' function to 'query' struct
                queryStruct.addFunctions(
                    mapOf(
                        "pokemon" to java.util.function.Function { params ->
                            return@Function QueryStruct(hashMapOf(
                                "species" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.species.resourceIdentifier.toString())
                                },
                                "form" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.form.name)
                                },
                                "level" to java.util.function.Function { params ->
                                    DoubleValue(event.pokemon.level.toDouble())
                                },
                                "is_starter" to java.util.function.Function { params ->
                                    val data = player get JourneyDataObject
                                    val starterPokemon = data.starterPokemon
                                    DoubleValue(if (starterPokemon != null && starterPokemon == event.pokemon.uuid) 1.0 else 0.0)
                                },
                                "primary_type" to java.util.function.Function { params ->
                                    StringValue(event.pokemon.types.first().name)
                                }
                                // Add other fields if needed
                            ))
                        }
                    )
                )

                val filterExpression = subtask.getOrParseFilterExpression()
                val result = runtime.resolveBoolean(filterExpression)
                if (result) {
                    progress.progress += 1.0
                    if (progress.progress >= subtask.target) {
                        subtask.rewards.ifNotEmpty { it.forEach { it.parse(player) } }
                        data.activeSubtasksByEvent[subtask.event.name]?.remove(progress)
                        data.checkAndCompleteTask(taskId, subtask, progress)
                    }
                }
            }
        }
    }

    fun createMolangRuntime(): MoLangRuntime {
        val runtime = MoLangRuntime().setup()
        return runtime
    }

    fun init() {}
}

private fun <E> List<E>.ifNotEmpty(function: (List<E>) -> Unit) {
    if (this.isNotEmpty() && this.any { it != null }) {
        function(this)
    }
}
