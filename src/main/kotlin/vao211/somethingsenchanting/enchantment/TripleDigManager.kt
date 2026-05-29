package vao211.somethingsenchanting.enchantment

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Direction

object TripleDigManager {
    private val isDigging = ThreadLocal.withInitial { false }

    fun init() {
        PlayerBlockBreakEvents.BEFORE.register { world, player, pos, state, blockEntity ->
            if (player !is ServerPlayerEntity || isDigging.get() || player.isSneaking) {
                return@register true
            }

            val stack = player.mainHandStack
            if (stack.isEmpty) return@register true

            val registryManager = world.registryManager
            val optionalEnchantment = registryManager.get(RegistryKeys.ENCHANTMENT)
                .getEntry(Identifier.of("somethingsenchanting", "triple_dig"))

            if (optionalEnchantment.isEmpty || EnchantmentHelper.getLevel(optionalEnchantment.get(), stack) <= 0) {
                return@register true
            }

            val reach = player.getAttributeValue(EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE)
            val hitResult = player.raycast(reach, 1.0f, false) as? BlockHitResult
            if (hitResult == null || hitResult.type == HitResult.Type.MISS) return@register true

            val face = hitResult.side

            isDigging.set(true)
            try {
                val (dx, dy, dz) = when (face.axis) {
                    Direction.Axis.Y -> Triple(1, 0, 1)
                    Direction.Axis.Z -> Triple(1, 1, 0)
                    Direction.Axis.X -> Triple(0, 1, 1)
                    else -> Triple(1, 1, 1)
                }

                for (x in -dx..dx) {
                    for (y in -dy..dy) {
                        for (z in -dz..dz) {
                            if (x == 0 && y == 0 && z == 0) continue
                            val targetPos = pos.add(x, y, z)
                            val targetState = world.getBlockState(targetPos)
                            if (targetState.isAir || targetState.getHardness(world, targetPos) < 0f) continue
                            if (!stack.isSuitableFor(targetState)) continue
                            player.interactionManager.tryBreakBlock(targetPos)
                        }
                    }
                }
            } finally {
                isDigging.set(false)
            }
            return@register true
        }
    }
}