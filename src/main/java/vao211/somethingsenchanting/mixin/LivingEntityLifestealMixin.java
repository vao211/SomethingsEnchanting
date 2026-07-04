package vao211.somethingsenchanting.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vao211.somethingsenchanting.enchantment.LifestealManager;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLifestealMixin {

    @Inject(method = "damage", at = @At("RETURN"))
    private void onTakeDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {

        if (cir.getReturnValue()) {
            if (source.getAttacker() instanceof ServerPlayerEntity player) {

                ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);

                if (!helmet.isEmpty() && helmet.hasEnchantments()) {
                    var registryManager = player.getWorld().getRegistryManager();
                    var optionalEnchantment = registryManager.get(RegistryKeys.ENCHANTMENT)
                            .getEntry(Identifier.of("somethingsenchanting", "lifesteal"));

                    if (optionalEnchantment.isPresent()) {
                        int level = EnchantmentHelper.getLevel(optionalEnchantment.get(), helmet);

                        if (level > 0) {
                            LifestealManager.INSTANCE.triggerLifesteal(player, level);
                        }
                    }
                }
            }
        }
    }
}