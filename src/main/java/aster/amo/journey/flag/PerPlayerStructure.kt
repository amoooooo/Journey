package aster.amo.journey.flag

import aster.amo.journey.Journey
import aster.amo.journey.mixin.StructureTemplateAccessor
import aster.amo.journey.toBlockPos
import aster.amo.journey.toVector3i
import aster.amo.journey.utils.MolangUtils
import com.bedrockk.molang.runtime.MoLangRuntime
import com.cobblemon.mod.common.api.molang.MoLangFunctions.setup
import com.cobblemon.mod.common.util.*
import com.google.gson.annotations.SerializedName
import it.unimi.dsi.fastutil.shorts.ShortOpenHashSet
import it.unimi.dsi.fastutil.shorts.ShortSet
import kotlinx.coroutines.*
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import org.joml.Vector3f
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

class PerPlayerStructure(
    val structure: String = "",
    @SerializedName("passed_structure") val passedStructure: String = "",
    val dimension: String = "",
    val position: Vector3f = Vector3f(),
    val script: String = ""
) {
    @Transient val refreshedPlayers: MutableList<UUID> = mutableListOf()
    @Transient var shortSet: ShortSet = ShortOpenHashSet()
    companion object PerPlayerStructures {
        val structures = mutableListOf<PerPlayerStructure>()
        @OptIn(DelicateCoroutinesApi::class)
        private val structureThreadPool = newFixedThreadPoolContext(1, "StructureThread")
        private val structureScope = CoroutineScope(structureThreadPool + SupervisorJob())

        fun send(server: MinecraftServer) {
            structureScope.launch {
                structures.forEach { structure ->
                    server.playerList.players.forEach { player ->
                        val runtime = MoLangRuntime().setup()
                        MolangUtils.setupPlayerStructs(runtime.environment.query, player)
                        val pass = runtime.resolveBoolean(structure.script.asExpressionLike())
                        if (pass) {
                            structure.refreshedPlayers.remove(player.uuid)
                            structure.send(player, structure.structure)
                        } else if (!structure.refreshedPlayers.contains(player.uuid) && !pass) {
                            structure.remove(player)
                            structure.refreshedPlayers.add(player.uuid)
                        } else {
                            structure.send(player, structure.passedStructure)
                        }
                    }
                }
            }
        }
    }

    val key: ResourceKey<Level> by lazy { ResourceKey.create(Registries.DIMENSION, dimension.asResource()) }
    var structureTemplate: StructureTemplate? = null
    var passedStructureTemplate: StructureTemplate? = null
    fun send(player: ServerPlayer, structure: String) {
        val dimension = player.server.getLevel(key) ?: return
        if (player.level() != dimension) return

        if (structureTemplate == null && structure == this.structure) {
            server()!!.execute {
                val structureManager = dimension.structureManager
                val structure = structureManager.get(this.structure.asResource())
                val template: StructureTemplate = structure.getOrNull() ?: return@execute
                structureTemplate = template
            }
        }
        if (passedStructureTemplate == null && structure == this.passedStructure && this.passedStructure.isNotEmpty()) {
            server()!!.execute {
                val structureManager = dimension.structureManager
                val structure = structureManager.get(this.passedStructure.asResource())
                val template: StructureTemplate = structure.getOrNull() ?: return@execute
                passedStructureTemplate = template
            }

        } else if( this.passedStructure.isEmpty() && structure == this.passedStructure) {
            remove(player)
            return
        }
        val templateToUse = if (structure == this.structure) structureTemplate else passedStructureTemplate
        val blockChanges = mutableMapOf<SectionPos, MutableList<Pair<StructureTemplate.StructureBlockInfo, BlockPos>>>()
        val origin = BlockPos(position.x().toInt(), position.y().toInt(), position.z().toInt())
        if(templateToUse == null) return
        (templateToUse as StructureTemplateAccessor).palettes.forEach { palette ->
            palette.blocks().forEach { blockInfo ->
                if(blockInfo.state.`is`(Blocks.STRUCTURE_BLOCK) || blockInfo.state.`is`(Blocks.STRUCTURE_VOID)) return@forEach
                val pos = blockInfo.pos.offset(origin)
                val sectionPos = SectionPos.of(pos)
                blockChanges.computeIfAbsent(sectionPos) { mutableListOf() }.add(blockInfo to pos)
            }
        }

        val biomeRegistry = dimension.registryAccess().registryOrThrow(Registries.BIOME)
        blockChanges.forEach { (sectionPos, changes) ->
            val shortSet: ShortSet = ShortOpenHashSet()
            val chunkSection = LevelChunkSection(biomeRegistry)
            changes.forEach { (blockInfo, pos) ->
                val localX = pos.x and 15
                val localY = pos.y and 15
                val localZ = pos.z and 15
                val blockState = blockInfo.state

                chunkSection.setBlockState(localX, localY, localZ, blockState)
                val shortPos = SectionPos.sectionRelativePos(BlockPos(localX, localY, localZ))
                shortSet.add(shortPos)
            }
            chunkSection.recalcBlockCounts()
            val packet = ClientboundSectionBlocksUpdatePacket(sectionPos, shortSet, chunkSection)
            player.connection.send(packet)
        }

    }

    fun remove(player: ServerPlayer) {
        this.shortSet.forEach { shortPos ->
            val x = shortPos.toInt() and 15
            val y = shortPos.toInt() shr 8 and 15
            val z = shortPos.toInt() shr 4 and 15
            player.level().sendBlockUpdated(position.toVec3d().add(x.toDouble(), y.toDouble(), z.toDouble()).toBlockPos(), Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), 3)
        }
    }

}