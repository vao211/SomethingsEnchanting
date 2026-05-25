package vao211.somethingsenchanting.enchantment

import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.enchantment.EnchantmentEffectContext
import net.minecraft.enchantment.effect.EnchantmentEntityEffect
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

data class EnsnareEffect(val durationTicks: Int) : EnchantmentEntityEffect {

    companion object {
        val CODEC: MapCodec<EnsnareEffect> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                com.mojang.serialization.Codec.INT.fieldOf("duration_ticks").forGetter(EnsnareEffect::durationTicks)
            ).apply(instance, ::EnsnareEffect)
        }
    }

    override fun apply(world: ServerWorld, level: Int, context: EnchantmentEffectContext, target: Entity, pos: Vec3d) {
        if (target is LivingEntity) {
            target.addStatusEffect(StatusEffectInstance(StatusEffects.SLOWNESS, durationTicks, 10, false, false))
            target.addStatusEffect(StatusEffectInstance(StatusEffects.JUMP_BOOST, durationTicks, 200, false, false))
        }
    }

    override fun getCodec(): MapCodec<out EnchantmentEntityEffect> {
        return CODEC
    }
}