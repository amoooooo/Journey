package aster.amo.journey.commands.subcommands

import aster.amo.ceremony.utils.extension.get
import aster.amo.journey.Journey
import aster.amo.journey.data.JourneyDataObject
import com.cobblemon.mod.common.util.toBlockPos
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import aster.amo.journey.utils.SubCommand

import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.coordinates.Vec3Argument
import net.minecraft.world.entity.player.Player
import aster.amo.journey.zones.Zone
import aster.amo.journey.zones.area.ZoneBox
import aster.amo.journey.zones.area.ZoneCylinder
import aster.amo.journey.zones.area.ZoneSphere
import java.util.UUID

class ZoneCommand : SubCommand {
    override fun build(): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("zone")
            .then(
                Commands.literal("show")
                    .executes { ctx ->
                        val player = ctx.source.playerOrException
                        (player as Player)[JourneyDataObject].showZoneBounds = !(player[JourneyDataObject].showZoneBounds)
                        1
                    }
            )
            .then(
                Commands.literal("create")
                    .then(
                        Commands.argument("name", StringArgumentType.string())
                            .then(
                                Commands.literal("box")
                                    .then(
                                        Commands.argument("pos1", Vec3Argument.vec3())
                                            .then(
                                                Commands.argument("pos2", Vec3Argument.vec3())
                                                    .executes { ctx ->
                                                        executeCreateZone(ctx, "box")
                                                    }
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("sphere")
                                    .then(
                                        Commands.argument("center", Vec3Argument.vec3())
                                            .then(
                                                Commands.argument("radius", DoubleArgumentType.doubleArg())
                                                    .executes { ctx ->
                                                        executeCreateZone(ctx, "sphere")
                                                    }
                                            )
                                    )
                            )
                            .then(
                                Commands.literal("cylinder")
                                    .then(
                                        Commands.argument("center", Vec3Argument.vec3())
                                            .then(
                                                Commands.argument("radius", DoubleArgumentType.doubleArg())
                                                    .then(
                                                        Commands.argument("height", DoubleArgumentType.doubleArg())
                                                            .executes { ctx ->
                                                                executeCreateZone(ctx, "cylinder")
                                                            }
                                                    )
                                            )
                                    )
                            )
                    )
            )
            // ... (rest of the command structure for delete, edit, etc.)
            .build()
    }

    private fun executeCreateZone(ctx: com.mojang.brigadier.context.CommandContext<CommandSourceStack>, type: String): Int {
        val name = StringArgumentType.getString(ctx, "name")

        val area = when (type) {
            "box" -> {
                val pos1 = Vec3Argument.getVec3(ctx, "pos1")
                val pos2 = Vec3Argument.getVec3(ctx, "pos2")
                ZoneBox(pos1, pos2, ctx.source.level.dimension().location().toString())
            }
            "sphere" -> {
                val center = Vec3Argument.getVec3(ctx, "center").toBlockPos()
                val radius = DoubleArgumentType.getDouble(ctx, "radius")
                ZoneSphere(center, radius, ctx.source.level.dimension().location().toString())
            }
            "cylinder" -> {
                val center = Vec3Argument.getVec3(ctx, "center").toBlockPos()
                val radius = DoubleArgumentType.getDouble(ctx, "radius")
                val height = DoubleArgumentType.getDouble(ctx, "height")
                ZoneCylinder(center, radius, height, ctx.source.level.dimension().location().toString())
            }
            else -> null // Shouldn't happen, but handle it just in case
        }

        if (area != null) {
            val zone = Zone(name, areas = mutableListOf(area))
            Journey.INSTANCE.zoneManager.zones.add(zone)
            return 1
        }
        return 0
    }
}
