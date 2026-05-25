package vao211.somethingsenchanting.enchantment

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.enchantment.EnchantmentEffectContext
import net.minecraft.enchantment.effect.EnchantmentEntityEffect
import net.minecraft.entity.Entity
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Vec3d
import java.util.WeakHashMap

object BerserkerManager {
    val COOLDOWNS = WeakHashMap<ServerPlayerEntity, Long>()
}

data class BerserkerEffect(val cooldownTicks: Int, val healthThreshold: Float) : EnchantmentEntityEffect {

    companion object {
        val CODEC: MapCodec<BerserkerEffect> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("cooldown_ticks").forGetter(BerserkerEffect::cooldownTicks),
                Codec.FLOAT.fieldOf("health_threshold").forGetter(BerserkerEffect::healthThreshold)
            ).apply(instance, ::BerserkerEffect)
        }
    }

    override fun apply(world: ServerWorld, level: Int, context: EnchantmentEffectContext, target: Entity, pos: Vec3d) {
        if (target !is ServerPlayerEntity) return
        if (target.health > healthThreshold) return

        val currentTime = world.time
        val lastTrigger = BerserkerManager.COOLDOWNS.getOrDefault(target, 0L)
        if (currentTime - lastTrigger < cooldownTicks) return

        BerserkerManager.COOLDOWNS[target] = currentTime

        val stack = context.stack
        if (stack.isDamageable) {
            val currentDurability = stack.maxDamage - stack.damage
            val damageToTake = currentDurability / 5

            if (damageToTake > 0) {
                stack.damage(damageToTake, world, target) { brokenItem ->
                    target.sendEquipmentBreakStatus(brokenItem, EquipmentSlot.CHEST)
                }
            }
        }

        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.getBodyY(0.5), target.z, 1, 0.0, 0.0, 0.0, 0.0)

        val enemies = world.getOtherEntities(target, target.boundingBox.expand(5.0))
        for (enemy in enemies) {
            if (enemy is LivingEntity) {
                val dx = enemy.x - target.x
                val dz = enemy.z - target.z
                enemy.takeKnockback(1.5, -dx, -dz)
            }
        }

        target.addStatusEffect(StatusEffectInstance(StatusEffects.STRENGTH, 100, 2))
        target.addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 100, 2))
        target.sendMessage(net.minecraft.text.Text.literal("Berserkinggggggg"), true)
    }

    override fun getCodec(): MapCodec<out EnchantmentEntityEffect> {
        return CODEC
    }
}