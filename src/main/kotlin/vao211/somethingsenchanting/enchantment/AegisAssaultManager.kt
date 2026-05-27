package vao211.somethingsenchanting.enchantment

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.LivingEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d
import java.util.UUID
import java.util.WeakHashMap
import net.minecraft.entity.player.PlayerEntity

data class DashState(var ticksLeft: Int, val level: Int, val hitEntities: MutableSet<UUID> = mutableSetOf())

object AegisAssaultManager {
    data class BowlingState(var ticksLeft: Int, val damage: Float, val attacker: ServerPlayerEntity)

    val BOWLING_ENTITIES = WeakHashMap<LivingEntity, BowlingState>()
    private val DASHING_PLAYERS = WeakHashMap<ServerPlayerEntity, DashState>()
    private val COOLDOWNS = WeakHashMap<ServerPlayerEntity, Long>()
    private val PREVIOUS_SNEAK_STATES = WeakHashMap<ServerPlayerEntity, Boolean>()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerManager.playerList) {
                val world = player.world as ServerWorld
                val isSneaking = player.isSneaking
                val wasSneaking = PREVIOUS_SNEAK_STATES.getOrDefault(player, false)

                if (isSneaking && !wasSneaking && player.isUsingItem) {
                    val activeItem = player.activeItem
                    val registry = world.registryManager.get(RegistryKeys.ENCHANTMENT)
                    val enchantEntry = registry.getEntry(Identifier.of("somethingsenchanting", "aegis_assault"))

                    if (enchantEntry.isPresent) {
                        val level = EnchantmentHelper.getLevel(enchantEntry.get(), activeItem)
                        if (level > 0) {
                            val currentTime = world.time
                            if (currentTime - COOLDOWNS.getOrDefault(player, 0L) >= 100) { // Hồi chiêu 5 giây
                                COOLDOWNS[player] = currentTime
                                DASHING_PLAYERS[player] = DashState(10, level)

                                val look = player.rotationVector
                                val dashVec = Vec3d(look.x, 0.0, look.z).normalize().multiply(1.8)
                                player.addVelocity(dashVec.x, 0.2, dashVec.z)
                                player.velocityModified = true

                                world.playSound(null as PlayerEntity?, player.x, player.y, player.z, SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.5f)
                            }
                        }
                    }
                }
                PREVIOUS_SNEAK_STATES[player] = isSneaking

                val dashState = DASHING_PLAYERS[player]
                if (dashState != null) {
                    dashState.ticksLeft--
                    world.spawnParticles(ParticleTypes.CLOUD, player.x, player.y, player.z, 5, 0.5, 0.0, 0.5, 0.0)

                    val box = player.boundingBox.expand(1.5, 0.5, 1.5)
                    val entities = world.getOtherEntities(player, box)
                    for (entity in entities) {
                        if (entity is LivingEntity && !dashState.hitEntities.contains(entity.uuid)) {
                            dashState.hitEntities.add(entity.uuid)

                            val damageAmount = 6.0f + (dashState.level * 2.0f)
                            entity.damage(world.damageSources.playerAttack(player), damageAmount)

                            val dx = entity.x - player.x
                            val dz = entity.z - player.z
                            val push = Vec3d(dx, 0.0, dz).normalize().multiply(1.0)
                            entity.addVelocity(push.x, 0.7, push.z)
                            entity.velocityModified = true

                            world.playSound(null as PlayerEntity?, entity.x, entity.y, entity.z, SoundEvents.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 0.8f)
                        }
                    }
                    if (dashState.ticksLeft <= 0) DASHING_PLAYERS.remove(player)
                }
            }

            val bowlingIterator = BOWLING_ENTITIES.entries.iterator()
            while (bowlingIterator.hasNext()) {
                val entry = bowlingIterator.next()
                val projectile = entry.key
                val state = entry.value
                val world = projectile.world as ServerWorld

                state.ticksLeft--
                world.spawnParticles(ParticleTypes.CRIT, projectile.x, projectile.y + 1.0, projectile.z, 2, 0.3, 0.3, 0.3, 0.0)

                val box = projectile.boundingBox.expand(0.5, 0.5, 0.5)
                val hits = world.getOtherEntities(projectile, box)
                var hitSomeone = false

                for (victim in hits) {
                    if (victim is LivingEntity && victim != state.attacker) {
                        victim.damage(world.damageSources.mobAttack(projectile), state.damage)

                        val dx = victim.x - projectile.x
                        val dz = victim.z - projectile.z
                        val push = Vec3d(dx, 0.0, dz).normalize().multiply(0.5)
                        victim.addVelocity(push.x, 0.3, push.z)
                        victim.velocityModified = true

                        projectile.velocity = Vec3d.ZERO
                        projectile.velocityModified = true
                        hitSomeone = true

                        world.playSound(null as PlayerEntity?, victim.x, victim.y, victim.z, SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.PLAYERS, 0.8f, 1.2f)
                        break
                    }
                }

                if (hitSomeone || state.ticksLeft <= 0 || projectile.isOnGround) {
                    bowlingIterator.remove()
                }
            }
        }
    }
}