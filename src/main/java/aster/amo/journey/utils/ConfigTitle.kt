package aster.amo.journey.utils

import com.cobblemon.mod.common.api.toast.Toast
import com.google.gson.annotations.SerializedName
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.title.Title
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import aster.amo.ceremony.utils.parseMiniMessage
import java.time.Duration
import java.time.temporal.ChronoUnit
import aster.amo.journey.Journey
import aster.amo.ceremony.module.cobblemon.notification.NotificationHandler.notification
import aster.amo.ceremony.utils.parseToNative
import aster.amo.ceremony.module.cobblemon.notification.NotificationHandler.NotificationTextures

class ConfigTitle(
    val title: String = "",
    val subtitle: String = "",
    @SerializedName("fade_in")
    val fadeIn: Int = 0,
    val stay: Int = 0,
    @SerializedName("fade_out")
    val fadeOut: Int = 0,
    val type: Type = Type.TITLE
) {
    enum class Type(val run: (Player, ConfigTitle) -> Unit) {
        TITLE({ player, config ->
            if(config.title.isNotEmpty() || config.subtitle.isNotEmpty()) {
                val title = Title.title(
                    config.title.parseMiniMessage(
                        TagResolver.resolver(
                            "player",
                            Tag.inserting(Component.text(player.name.string))
                        )
                    ),
                    config.subtitle.parseMiniMessage(
                        TagResolver.resolver(
                            "player",
                            Tag.inserting(Component.text(player.name.string))
                        )
                    ),
                    Title.Times.times(
                        Duration.of(config.fadeIn.toLong(), ChronoUnit.SECONDS),
                        Duration.of(config.stay.toLong(), ChronoUnit.SECONDS),
                        Duration.of(config.fadeOut.toLong(), ChronoUnit.SECONDS)
                    )
                )
                Journey.INSTANCE.adventure.audience(player).showTitle(title)
            }
        }),
        ACTION_BAR({ player, config ->
            if(config.title.isNotEmpty() || config.subtitle.isNotEmpty()) {
                player.displayClientMessage(
                    config.title.parseToNative(
                        TagResolver.resolver(
                            "player",
                            Tag.inserting(Component.text(player.name.string))
                        )
                    ),
                    true
                )
            }
        }),
        TOAST({ player, config ->
            if(config.title.isNotEmpty() || config.subtitle.isNotEmpty()) {
                notification {
                    this.title = config.title.parseToNative(
                        TagResolver.resolver(
                            "player",
                            Tag.inserting(Component.text(player.name.string))
                        )
                    ).copy()
                    this.description = config.subtitle.parseToNative(
                        TagResolver.resolver(
                            "player",
                            Tag.inserting(Component.text(player.name.string))
                        )
                    ).copy()
                    this.icon = Items.SPYGLASS.defaultInstance
                    this.duration = (config.fadeIn + config.stay + config.fadeOut)
                    this.texture = Toast.VANILLA_FRAME
                    this.progressColor = Toast.VANILLA_PROGRESS_COLOR
                }.send(player as ServerPlayer)
            }
        })
    }
}
