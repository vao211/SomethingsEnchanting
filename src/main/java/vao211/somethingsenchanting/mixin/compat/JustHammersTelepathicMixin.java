package vao211.somethingsenchanting.mixin.compat;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

@Mixin(targets = {"pro.mikey.justhammers.HammerItem"})
public abstract class JustHammersTelepathicMixin {
    @Redirect(
            method = "findAndBreakNearBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/Block;getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;"
            )
    )
    private List<ItemStack> interceptHammerDrops(
            BlockState state,
            ServerWorld world,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity entity,
            ItemStack tool
    ) {
        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, blockEntity, entity, tool);
        if (entity instanceof ServerPlayerEntity player) {
            boolean hasTelepathic = false;
            var enchantments = tool.getEnchantments().getEnchantmentEntries();
            for (var entry : enchantments) {
                var registryKeyOpt = entry.getKey().getKey();
                if (registryKeyOpt.isPresent()) {
                    var registryKey = registryKeyOpt.get();
                    if (registryKey.getValue().getNamespace().equals("somethingsenchanting") &&
                            registryKey.getValue().getPath().equals("telepathic")) {

                        if (entry.getIntValue() > 0) {
                            hasTelepathic = true;
                            break;
                        }
                    }
                }
            }

            if (hasTelepathic && !player.getCommandTags().contains("SomethingsAddons_PickupLocked")) {
                for (ItemStack drop : drops) {
                    if (!player.getInventory().insertStack(drop)) {
                        player.dropItem(drop, false);
                    }
                }
                return Collections.emptyList();
            }
        }
        return drops;
    }
}