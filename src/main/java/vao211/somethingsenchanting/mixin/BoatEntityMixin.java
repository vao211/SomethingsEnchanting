package vao211.somethingsenchanting.mixin;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BoatEntity.class)
public abstract class BoatEntityMixin extends VehicleEntity {

    public BoatEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }
    @Inject(method = "updateVelocity", at = @At("TAIL"))
    private void applyWakeRiderBoost(CallbackInfo ci) {
        if (this.getFirstPassenger() instanceof LivingEntity driver) {
            ItemStack pants = driver.getEquippedStack(EquipmentSlot.LEGS);
            if (!pants.isEmpty()) {

                var registryManager = this.getWorld().getRegistryManager();
                var optionalEnchantment = registryManager.get(RegistryKeys.ENCHANTMENT)
                        .getEntry(Identifier.of("somethingsenchanting", "wake_rider"));

                if (optionalEnchantment.isPresent()) {
                    int level = EnchantmentHelper.getLevel(optionalEnchantment.get(), pants);

                    if (level > 0) {
                        float boostFactor = 1.0f + (0.02f * level);
                        Vec3d currentVel = this.getVelocity();
                        this.setVelocity(currentVel.x * boostFactor, currentVel.y, currentVel.z * boostFactor);
                    }
                }
            }
        }
    }
}