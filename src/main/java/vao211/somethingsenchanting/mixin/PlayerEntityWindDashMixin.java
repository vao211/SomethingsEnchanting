package vao211.somethingsenchanting.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vao211.somethingsenchanting.compat.trinkets.TrinketsHelper;
import vao211.somethingsenchanting.enchantment.WindDashManager;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityWindDashMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void somethingsenchanting$onPlayerTick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (player.getWorld().isClient() || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        if (!player.isFallFlying()) {
            WindDashManager.INSTANCE.getLAST_SNEAK_STATE().put(serverPlayer, player.isSneaking());
            return;
        }

        ItemStack elytra = player.getEquippedStack(EquipmentSlot.CHEST);

        if (!elytra.isOf(Items.ELYTRA) && FabricLoader.getInstance().isModLoaded("trinkets")) {
            elytra = TrinketsHelper.findElytraInTrinkets(player);
        }

        if (!elytra.isEmpty() && elytra.hasEnchantments()) {
            var registryManager = player.getWorld().getRegistryManager();
            var optionalEnchant = registryManager.get(RegistryKeys.ENCHANTMENT)
                    .getEntry(Identifier.of("somethingsenchanting", "wind_dash"));

            if (optionalEnchant.isPresent()) {
                int level = EnchantmentHelper.getLevel(optionalEnchant.get(), elytra);
                if (level > 0) {
                    WindDashManager.INSTANCE.triggerWindDash(serverPlayer, elytra, level);
                }
            }
        }
    }
}