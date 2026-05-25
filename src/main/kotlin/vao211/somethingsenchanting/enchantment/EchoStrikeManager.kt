package vao211.somethingsenchanting.enchantment

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import java.util.WeakHashMap

data class PendingStrike(val target: LivingEntity, val source: ServerPlayerEntity, val damage: Float, var ticksLeft: Int)

object EchoStrikeManager {
    private val COOLDOWNS = WeakHashMap<ServerPlayerEntity, Long>()
    private val PENDING_STRIKES = mutableListOf<PendingStrike>()

    fun init() {
        ServerTickEvents.END_SERVER_TICK.register { _ ->
            val snapshot = PENDING_STRIKES.toList()

            for (strike in snapshot) {
                if (strike.source.isRemoved || strike.target.isRemoved) {
                    PENDING_STRIKES.remove(strike)
                    continue
                }

                strike.ticksLeft--

                if (strike.ticksLeft <= 0) {
                    if (strike.target.isAlive && strike.source.isAlive) {
                        val world = strike.target.world as ServerWorld

                        world.spawnParticles(ParticleTypes.SWEEP_ATTACK, strike.target.x, strike.target.getBodyY(0.5), strike.target.z, 1, 0.0, 0.0, 0.0, 0.0)
                        world.playSound(null, strike.target.blockPos, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.5f)
                        strike.target.timeUntilRegen = 0
                        val damageSource = world.damageSources.playerAttack(strike.source)
                        strike.target.damage(damageSource, strike.damage)
                    }
                    PENDING_STRIKES.remove(strike)
                }
            }
        }
    }

    fun tryScheduleStrike(player: ServerPlayerEntity, target: LivingEntity, currentWorldTime: Long, delayTicks: Int, cooldownTicks: Int, damageMultiplier: Float) {
        val lastEchoTriggerTime = COOLDOWNS.getOrDefault(player, 0L)

        if (currentWorldTime - lastEchoTriggerTime >= cooldownTicks) {
            val baseDamage = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE).toFloat()
            val finalDamage = baseDamage * damageMultiplier

            PENDING_STRIKES.add(PendingStrike(target, player, finalDamage, delayTicks))
            COOLDOWNS[player] = currentWorldTime
        }
    }
}