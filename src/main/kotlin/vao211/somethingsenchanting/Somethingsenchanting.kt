package vao211.somethingsenchanting

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import vao211.somethingsenchanting.enchantment.AegisAssaultManager
import vao211.somethingsenchanting.enchantment.AegisAssaultEffect
import vao211.somethingsenchanting.enchantment.BerserkerEffect
import vao211.somethingsenchanting.enchantment.EarthshatterEffect
import vao211.somethingsenchanting.enchantment.EchoStrikeEffect
import vao211.somethingsenchanting.enchantment.EchoStrikeManager
import vao211.somethingsenchanting.enchantment.EnsnareEffect
import vao211.somethingsenchanting.enchantment.SlideTackleManager
import vao211.somethingsenchanting.enchantment.SplinterPinManager
import vao211.somethingsenchanting.enchantment.TripleDigManager
import vao211.somethingsenchanting.item.CustomExpBottleItem

class Somethingsenchanting : ModInitializer {

    override fun onInitialize() {
        EchoStrikeManager.init()
        SlideTackleManager.init()
        SplinterPinManager.init()
        AegisAssaultManager.init()
        TripleDigManager.init()

        Registry.register(
            Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.of("somethingsenchanting", "ensnare"),
            EnsnareEffect.CODEC
        )
        Registry.register(
            Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.of("somethingsenchanting", "echo_strike"),
            EchoStrikeEffect.CODEC
        )
        Registry.register(
            Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.of("somethingsenchanting", "berserker"),
            BerserkerEffect.CODEC
        )
        Registry.register(
            Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.of("somethingsenchanting", "earth_shatter"),
            EarthshatterEffect.CODEC
        )

        Registry.register(
            Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.of("somethingsenchanting", "aegis_assault"),
            AegisAssaultEffect.CODEC
        )

        val MEDIUM_EXP_BOTTLE = Registry.register(
            Registries.ITEM,
            Identifier.of("somethingsenchanting", "medium_exp_bottle"),
            CustomExpBottleItem(Item.Settings().maxCount(64), 3)
        )

        val BIG_EXP_BOTTLE = Registry.register(
            Registries.ITEM,
            Identifier.of("somethingsenchanting", "big_exp_bottle"),
            CustomExpBottleItem(Item.Settings().maxCount(64), 9)
        )

        val HUGE_EXP_BOTTLE = Registry.register(
            Registries.ITEM,
            Identifier.of("somethingsenchanting", "huge_exp_bottle"),
            CustomExpBottleItem(Item.Settings().maxCount(64), 30)
        )

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register { entries ->
            entries.add(MEDIUM_EXP_BOTTLE)
            entries.add(BIG_EXP_BOTTLE)
            entries.add(HUGE_EXP_BOTTLE)
        }
    }
}
