package vao211.somethingsenchanting.client

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EquipmentSlot
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Identifier

class SomethingsenchantingClient : ClientModInitializer {
    private var wasJumpPressed = false
    private var hasDoubleJumped = false
    private var jumpCooldown = 0

    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            val player = client.player ?: return@register
            val world = client.world ?: return@register

            if (jumpCooldown > 0) {
                jumpCooldown--
            }

            val isJumpPressed = client.options.jumpKey.isPressed
            if (player.isOnGround || player.isClimbing || player.isTouchingWater) {
                hasDoubleJumped = false
            }
            else if (!wasJumpPressed && isJumpPressed) {
                if (!hasDoubleJumped && jumpCooldown <= 0 && !player.abilities.flying) {
                    val enchantmentRegistry = world.registryManager.get(RegistryKeys.ENCHANTMENT)
                    val enchantEntry = enchantmentRegistry.getEntry(Identifier.of("somethingsenchanting", "double_jump"))
                    if (enchantEntry.isPresent) {
                        val boots = player.getEquippedStack(EquipmentSlot.FEET)
                        val level = EnchantmentHelper.getLevel(enchantEntry.get(), boots)

                        if (level > 0) {
                            val currentVelocity = player.velocity
                            player.setVelocity(currentVelocity.x, 0.5, currentVelocity.z)
                            player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f)
                            hasDoubleJumped = true
                            jumpCooldown = 10
                        }
                    }
                }
            }
            wasJumpPressed = isJumpPressed
        }
    }
}