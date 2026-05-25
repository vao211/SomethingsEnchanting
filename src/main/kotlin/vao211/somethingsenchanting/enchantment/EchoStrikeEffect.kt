package vao211.somethingsenchanting.enchantment

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.enchantment.EnchantmentEffectContext
import net.minecraft.enchantment.effect.EnchantmentEntityEffect
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

data class EchoStrikeEffect(val delayTicks: Int, val cooldownTicks: Int, val damageMultiplier: Float) : EnchantmentEntityEffect {

    companion object {
        val CODEC: MapCodec<EchoStrikeEffect> = RecordCodecBuilder.mapCodec { instance ->
            instance.group(
                Codec.INT.fieldOf("delay_ticks").forGetter(EchoStrikeEffect::delayTicks),
                Codec.INT.fieldOf("cooldown_ticks").forGetter(EchoStrikeEffect::cooldownTicks),
                Codec.FLOAT.fieldOf("damage_multiplier").forGetter(EchoStrikeEffect::damageMultiplier)
            ).apply(instance, ::EchoStrikeEffect)
        }
    }

    override fun apply(world: ServerWorld, level: Int, context: EnchantmentEffectContext, target: Entity, pos: Vec3d) {
        val owner = context.owner
        if (owner is ServerPlayerEntity && target is LivingEntity) {
            val currentTime = world.time
            val totalMultiplier = damageMultiplier * level

            EchoStrikeManager.tryScheduleStrike(owner, target, currentTime, delayTicks, cooldownTicks, totalMultiplier)
        }
    }

    override fun getCodec(): MapCodec<out EnchantmentEntityEffect> {
        return CODEC
    }
}