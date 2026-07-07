package vao211.somethingsenchanting.compat.trinkets;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class TrinketsHelper {
    public static ItemStack findElytraInTrinkets(PlayerEntity player) {
        var optionalTrinket = TrinketsApi.getTrinketComponent(player);
        if (optionalTrinket.isPresent()) {
            var equipped = optionalTrinket.get().getEquipped(Items.ELYTRA);
            if (!equipped.isEmpty()) {
                return equipped.get(0).getRight();
            }
        }
        return ItemStack.EMPTY;
    }
}