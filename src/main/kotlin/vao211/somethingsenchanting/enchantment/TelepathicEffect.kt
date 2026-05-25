package vao211.somethingsenchanting.enchantment

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld

//Handle in mixin
object TelepathicLogic {
    fun handleBlockDrops(world: ServerWorld, player: PlayerEntity, stack: ItemStack) {
        if (!player.inventory.insertStack(stack)) {
            player.dropItem(stack, false)
        }
    }
}