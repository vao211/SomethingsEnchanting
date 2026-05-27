package vao211.somethingsenchanting.enchantment

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
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

data class SlideState(var ticksLeft: Int, val level: Int, val hitEntities: MutableSet<UUID> = mutableSetOf())

object SlideTackleManager {
    private val PREVIOUS_SNEAK_STATES = WeakHashMap<ServerPlayerEntity, Boolean>()
    private val COOLDOWNS = WeakHashMap<ServerPlayerEntity, Long>()
    private val SLIDING_PLAYERS = WeakHashMap<ServerPlayerEntity, SlideState>()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerManager.playerList) {
                val world = player.world as ServerWorld
                val isSneaking = player.isSneaking
                val wasSneaking = PREVIOUS_SNEAK_STATES.getOrDefault(player, false)

                if (isSneaking && !wasSneaking && player.isSprinting && player.isOnGround) {
                    val currentTime = world.time
                    val lastSlide = COOLDOWNS.getOrDefault(player, 0L)

                    if (currentTime - lastSlide >= 60) {
                        val registry = world.registryManager.get(RegistryKeys.ENCHANTMENT)
                        val enchantEntry = registry.getEntry(Identifier.of("somethingsenchanting", "slide_tackle"))

                        if (enchantEntry.isPresent) {
                            val leggings = player.getEquippedStack(EquipmentSlot.LEGS)
                            val level = EnchantmentHelper.getLevel(enchantEntry.get(), leggings)

                            if (level > 0) {
                                COOLDOWNS[player] = currentTime
                                SLIDING_PLAYERS[player] = SlideState(10, level) // Trượt dài trong 10 ticks (0.5 giây)

                                val look = player.rotationVector
                                val dashVec = Vec3d(look.x, 0.0, look.z).normalize().multiply(1.5) // Lướt 3 block
                                player.addVelocity(dashVec.x, 0.0, dashVec.z)
                                player.velocityModified = true // Báo cho server đồng bộ lực đẩy này xuống Client

                                world.playSound(null as ServerPlayerEntity?, player.blockPos, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.5f)
                            }
                        }
                    }
                }

                PREVIOUS_SNEAK_STATES[player] = isSneaking

                val slideState = SLIDING_PLAYERS[player]
                if (slideState != null) {
                    slideState.ticksLeft--

                    world.spawnParticles(ParticleTypes.CLOUD, player.x, player.y, player.z, 3, 0.3, 0.0, 0.3, 0.0)

                    val box = player.boundingBox.expand(1.5, 0.5, 1.5)
                    val entities = world.getOtherEntities(player, box)

                    for (entity in entities) {
                        if (entity is LivingEntity && !slideState.hitEntities.contains(entity.uuid)) {
                            slideState.hitEntities.add(entity.uuid)

                            val damageAmount = 4.0f + (slideState.level * 2)
                            val damageSource = world.damageSources.playerAttack(player)
                            entity.damage(damageSource, damageAmount)
                            entity.addVelocity(0.0, 1.0, 0.0)
                            entity.velocityModified = true
                            entity.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2))
                            world.playSound(null as ServerPlayerEntity?, entity.blockPos, SoundEvents.ENTITY_SKELETON_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f)
                        }
                    }
                    if (slideState.ticksLeft <= 0) {
                        SLIDING_PLAYERS.remove(player)
                    }
                }
            }
        }
    }
}