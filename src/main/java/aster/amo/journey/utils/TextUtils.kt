package aster.amo.journey.utils

import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import aster.amo.journey.Journey
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer

object TextUtils {
    fun toNative(text: String): Component {
        return Journey.INSTANCE.adventure.toNative(Journey.MINI_MESSAGE.deserialize(text))
    }

    fun toComponent(text: String): net.kyori.adventure.text.Component {
        return Journey.MINI_MESSAGE.deserialize(text)
    }
}

fun net.kyori.adventure.text.Component.toNative(): Component {
    return Journey.INSTANCE.adventure.toNative(this).copy().withStyle { s -> s.withItalic(false) }
}

fun Component.toAdventure(): net.kyori.adventure.text.Component {
    return Journey.MINI_MESSAGE.deserialize(this.toString())
}

fun String.parseMiniMessage(vararg placeholders: TagResolver): net.kyori.adventure.text.Component {
    return Journey.MINI_MESSAGE.deserialize(this, *placeholders)
}


fun String.parseToNative(vararg placeholders: TagResolver): MutableComponent {
    return this.parseMiniMessage(*placeholders).toNative() as MutableComponent
}

infix fun ServerPlayer.inform(component: Component) {
    this.sendSystemMessage("<blue> ℹ <gold>".parseToNative().copy().append(component))
}
