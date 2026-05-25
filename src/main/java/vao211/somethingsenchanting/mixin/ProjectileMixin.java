package vao211.somethingsenchanting.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vao211.somethingsenchanting.enchantment.SplinterPinManager;

@Mixin(PersistentProjectileEntity.class)
public abstract class ProjectileMixin {
    @Shadow public abstract ItemStack getWeaponStack();
    @Shadow protected abstract ItemStack getItemStack();
    @Inject(method = "onEntityHit", at = @At("RETURN"))
    private void onHitTarget(EntityHitResult entityHitResult, CallbackInfo ci) {
        Entity target = entityHitResult.getEntity();
        if (target instanceof LivingEntity victim && !victim.getWorld().isClient()) {
            ItemStack weapon = this.getWeaponStack();
            if (weapon != null && !weapon.isEmpty()) {
                var registry = victim.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
                var enchant = registry.get(Identifier.of("somethingsenchanting", "splinter_pin"));

                if (enchant != null) {
                    int level = EnchantmentHelper.getLevel(registry.getEntry(enchant), weapon);
                    if (level > 0) {
                        ItemStack arrow = this.getItemStack();
                        SplinterPinManager.INSTANCE.addStuckArrow(victim, arrow.copy(), level);
                    }
                }
            }
        }
    }
}