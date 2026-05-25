package vao211.somethingsenchanting.enchantment

import net.minecraft.server.network.ServerPlayerEntity
import java.util.WeakHashMap

object LifestealManager {
    val WINDOWS = WeakHashMap<ServerPlayerEntity, Long>()
    val COOLDOWNS = WeakHashMap<ServerPlayerEntity, Long>()
}