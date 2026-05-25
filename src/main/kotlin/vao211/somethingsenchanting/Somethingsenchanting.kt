package vao211.somethingsenchanting

import net.fabricmc.api.ModInitializer
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
import vao211.somethingsenchanting.enchantment.LifestealEffect
import vao211.somethingsenchanting.enchantment.SlideTackleManager
import vao211.somethingsenchanting.enchantment.SplinterPinManager

class Somethingsenchanting : ModInitializer {

    override fun onInitialize() {
        EchoStrikeManager.init()
        SlideTackleManager.init()
        SplinterPinManager.init()
        AegisAssaultManager.init()


        Registry.register(
            Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
            Identifier.of("somethingsenchanting", "lifesteal"),
            LifestealEffect.CODEC
        )
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
    }
}
