package vao211.somethingsenchanting.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vao211.somethingsenchanting.enchantment.TelepathicLogic;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "dropStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"), cancellable = true)
    private static void injectTelepathic(BlockState state, net.minecraft.world.World world, BlockPos pos, BlockEntity blockEntity, Entity entity, ItemStack tool, CallbackInfo ci) {
        if (world instanceof ServerWorld serverWorld && entity instanceof PlayerEntity player) {

            if (player.getCommandTags().contains("SomethingsAddons_PickupLocked")) {
                return;
            }
            var registry = serverWorld.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
            var ensnareEnchant = registry.get(Identifier.of("somethingsenchanting", "telepathic"));

            if (ensnareEnchant != null && EnchantmentHelper.getLevel(registry.getEntry(ensnareEnchant), tool) > 0) {
                List<ItemStack> drops = Block.getDroppedStacks(state, serverWorld, pos, blockEntity, entity, tool);
                for (ItemStack drop : drops) {
                    TelepathicLogic.INSTANCE.handleBlockDrops(serverWorld, player, drop);
                }
                ci.cancel();
            }
        }
    }
}