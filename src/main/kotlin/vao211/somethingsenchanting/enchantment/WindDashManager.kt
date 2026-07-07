package vao211.somethingsenchanting.enchantment

import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import java.util.WeakHashMap

object WindDashManager {
    val LAST_SNEAK_STATE = WeakHashMap<ServerPlayerEntity, Boolean>()
    val COOLDOWNS = WeakHashMap<ServerPlayerEntity, Long>()

    fun triggerWindDash(player: ServerPlayerEntity, elytraStack: ItemStack, level: Int) {
        val isSneaking = player.isSneaking
        val wasSneaking = LAST_SNEAK_STATE.getOrDefault(player, false)
        LAST_SNEAK_STATE[player] = isSneaking

        if (!isSneaking || wasSneaking) return

        val world = player.serverWorld
        val currentTime = world.time
        val lastTrigger = COOLDOWNS.getOrDefault(player, 0L)

        if (currentTime - lastTrigger < 40) return

        if (elytraStack.isDamageable) {
            val currentDurability = elytraStack.maxDamage - elytraStack.damage
            if (currentDurability <= 11) {
                return
            }
            elytraStack.damage(10, world, player) { brokenItem ->
                player.sendEquipmentBreakStatus(brokenItem, EquipmentSlot.CHEST)
            }
        }

        COOLDOWNS[player] = currentTime

        val lookVec = player.rotationVector
        val force = level * 1.0

//      player.addVelocity(lookVec.x * force, lookVec.y * force, lookVec.z * force)
        player.velocity = Vec3d(lookVec.x * force, lookVec.y * force, lookVec.z * force)
        player.velocityModified = true

        world.playSound(null, player.x, player.y, player.z, SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.0f, 1.5f)
        world.spawnParticles(ParticleTypes.CLOUD, player.x, player.y, player.z, 15, 0.2, 0.2, 0.2, 0.1)
    }
}