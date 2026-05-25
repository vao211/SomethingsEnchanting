package vao211.somethingsenchanting.enchantment

import com.mojang.serialization.MapCodec
import net.minecraft.enchantment.EnchantmentEffectContext
import net.minecraft.enchantment.effect.EnchantmentEntityEffect
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.Vec3d

class AegisAssaultEffect : EnchantmentEntityEffect {
    companion object {
        val CODEC: MapCodec<AegisAssaultEffect> = MapCodec.unit(AegisAssaultEffect())
    }
    override fun apply(world: ServerWorld, level: Int, context: EnchantmentEffectContext, target: Entity, pos: Vec3d) {
        val attacker = context.owner
        if (attacker !is ServerPlayerEntity || target !is LivingEntity) return

        val bowlingDamage = when(level){
            1 -> 4.0f
            2 -> 8.0f
            3 -> 10.0f
            else -> 1.0f
        }
        val look = attacker.rotationVector
        target.takeKnockback(1.5, -look.x, -look.z)
        target.velocityModified = true

        AegisAssaultManager.BOWLING_ENTITIES[target] = AegisAssaultManager.BowlingState(15, bowlingDamage, attacker)
    }

    override fun getCodec(): MapCodec<out EnchantmentEntityEffect> = CODEC
}