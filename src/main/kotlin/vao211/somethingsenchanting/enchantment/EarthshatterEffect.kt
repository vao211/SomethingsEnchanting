package vao211.somethingsenchanting.enchantment

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.enchantment.EnchantmentEffectContext
import net.minecraft.enchantment.effect.EnchantmentEntityEffect
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import java.util.WeakHashMap
object EarthshatterManager {
    val COOLDOWNS = WeakHashMap<ServerPlayerEntity, Long>()
}

data class EarthshatterEffect(val cooldownTicks: Int, val fractionPerLevel: Float) : EnchantmentEntityEffect {

    companion object {
        val CODEC: MapCodec<EarthshatterEffect> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("cooldown_ticks").forGetter(EarthshatterEffect::cooldownTicks),
                Codec.FLOAT.fieldOf("fraction_per_level").forGetter(EarthshatterEffect::fractionPerLevel)
            ).apply(instance, ::EarthshatterEffect)
        }
    }

    override fun apply(world: ServerWorld, level: Int, context: EnchantmentEffectContext, target: Entity, pos: Vec3d) {
        val attacker = context.owner
        if (attacker !is ServerPlayerEntity || target !is LivingEntity) return
        val isJumpAttack = attacker.fallDistance > 0.0f && !attacker.isOnGround && !attacker.isClimbing && !attacker.isTouchingWater
        if (!isJumpAttack) return

        val currentTime = world.time
        val lastTrigger = EarthshatterManager.COOLDOWNS.getOrDefault(attacker, 0L)
        if (currentTime - lastTrigger < cooldownTicks) return

        EarthshatterManager.COOLDOWNS[attacker] = currentTime

        val baseDamage = attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE).toFloat() * 1.5f
        val aoeDamage = baseDamage * (fractionPerLevel * level)

        val box = target.boundingBox.expand(2.0, 1.0, 2.0)
        val entities = world.getOtherEntities(attacker, box)

        for (entity in entities) {
            if (entity is LivingEntity && entity != target) {
                val damageSource = world.damageSources.playerAttack(attacker)
                entity.damage(damageSource, aoeDamage)

                val dx = target.x - entity.x
                val dz = target.z - entity.z
                entity.takeKnockback(0.55, dx, dz)

                entity.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, 40, 4))
            }
        }

        world.playSound(null as ServerPlayerEntity?, target.blockPos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.8f, 0.5f)
        world.playSound(null as net.minecraft.entity.player.PlayerEntity?, target.x, target.y, target.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 0.7f)
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.x, target.getBodyY(0.1), target.z, 20, 1.5, 0.2, 1.5, 0.05)
        world.spawnParticles(ParticleTypes.EXPLOSION, target.x, target.getBodyY(0.5), target.z, 2, 0.5, 0.0, 0.5, 0.0)
    }

    override fun getCodec(): MapCodec<out EnchantmentEntityEffect> {
        return CODEC
    }
}