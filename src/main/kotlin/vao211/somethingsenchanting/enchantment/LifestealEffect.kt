package vao211.somethingsenchanting.enchantment

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.enchantment.EnchantmentEffectContext
import net.minecraft.enchantment.effect.EnchantmentEntityEffect
import net.minecraft.entity.Entity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d


data class LifestealEffect(
    val windowTicks: Int,
    val cooldownTicks: Int,
    val healFraction: Float
) : EnchantmentEntityEffect {

    companion object {
        val CODEC: MapCodec<LifestealEffect> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("window_ticks").forGetter(LifestealEffect::windowTicks),
                Codec.INT.fieldOf("cooldown_ticks").forGetter(LifestealEffect::cooldownTicks),
                Codec.FLOAT.fieldOf("heal_fraction").forGetter(LifestealEffect::healFraction)
            ).apply(instance, ::LifestealEffect)
        }
    }

    override fun apply(world: ServerWorld, level: Int, context: EnchantmentEffectContext, target: Entity, pos: Vec3d) {
        val attacker = target as? ServerPlayerEntity ?: return

        val currentTime = world.time
        val windowEnd = LifestealManager.WINDOWS.getOrDefault(attacker, 0L)
        val cooldownEnd = LifestealManager.COOLDOWNS.getOrDefault(attacker, 0L)

        if (currentTime > windowEnd && currentTime < cooldownEnd) {
            return
        }

        if (currentTime >= cooldownEnd) {
            LifestealManager.WINDOWS[attacker] = currentTime + windowTicks
            LifestealManager.COOLDOWNS[attacker] = currentTime + windowTicks + cooldownTicks
        }

        var attackDamage = attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE).toFloat()

        val isJumpAttack = attacker.fallDistance > 0.0f && !attacker.isOnGround && !attacker.isClimbing && !attacker.isTouchingWater
        if (isJumpAttack) {
            attackDamage *= 1.5f
        }

        val amountToHeal = attackDamage * healFraction * level

        if (attacker.health < attacker.maxHealth) {
            attacker.heal(amountToHeal)

            world.spawnParticles(ParticleTypes.HEART, attacker.x, attacker.y + 1.0, attacker.z, 3, 0.4, 0.4, 0.4, 0.0)
            world.playSound(null as net.minecraft.entity.player.PlayerEntity?, attacker.x, attacker.y, attacker.z, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 1.2f)
        }
    }

    override fun getCodec(): MapCodec<out EnchantmentEntityEffect> = CODEC
}