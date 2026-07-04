package vao211.somethingsenchanting.enchantment

import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.server.network.ServerPlayerEntity
import java.util.WeakHashMap

object LifestealManager {
    val HITS_TO_SKIP = WeakHashMap<ServerPlayerEntity, Int>()

    val LIFESTEAL_COUNT = WeakHashMap<ServerPlayerEntity, Int>()

    val COOLDOWNS = WeakHashMap<ServerPlayerEntity, Long>()

    fun triggerLifesteal(attacker: ServerPlayerEntity, level: Int) {
        val world = attacker.serverWorld
        val currentTime = world.time

        val cdEnd = COOLDOWNS.getOrDefault(attacker, 0L)
        if (currentTime < cdEnd) {
            return
        } else if (cdEnd != 0L) {
            COOLDOWNS[attacker] = 0L
            LIFESTEAL_COUNT[attacker] = 0
        }

        if (attacker.health >= attacker.maxHealth) {
            return
        }

        val currentSkip = HITS_TO_SKIP.getOrDefault(attacker, 0)
        if (currentSkip > 0) {
            HITS_TO_SKIP[attacker] = currentSkip - 1
            return
        }

        val attackDamage = attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE).toFloat()
        val attackSpeed = attacker.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED).toFloat()


        val heal = maxOf(2.0f, (attackDamage / (attackSpeed + 1.0f)))

        attacker.heal(heal)

        val currentCount = LIFESTEAL_COUNT.getOrDefault(attacker, 0) + 1
        LIFESTEAL_COUNT[attacker] = currentCount

        val maxLifesteals = 3 + (level - 1)

        if (currentCount >= maxLifesteals) {
            COOLDOWNS[attacker] = currentTime + 60L
        } else {
            HITS_TO_SKIP[attacker] = 2
        }
    }
}