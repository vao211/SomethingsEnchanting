package vao211.somethingsenchanting.enchantment

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.DisplayEntity.BillboardMode
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.world.World
import java.util.UUID
import java.util.WeakHashMap

private data class ExplodeStuckData(
    val arrows: MutableList<TimedArrow>,
    var indicator: TextDisplayEntity? = null,
    var enchantLevel: Int = 1
)

object ExplodeManager {
    private val STUCK_DATA = WeakHashMap<LivingEntity, ExplodeStuckData>()
    private val ACTIVE_INDICATORS = mutableSetOf<UUID>()
    private val PENDING_REMOVALS = mutableSetOf<Entity>()
    private const val INDICATOR_TAG = "explode_indicator"
    private const val TIMEOUT_TICKS = 240L
    private const val HEIGHT_OFFSET = 0.7
    private const val MAX_STACKS = 5

    fun addStuckArrow(victim: LivingEntity, arrow: ItemStack, level: Int) {
        if (victim.isRemoved || !victim.isAlive || victim.health <= 0f) return

        val world = victim.world as? ServerWorld ?: return
        val currentTime = world.time
        val data = STUCK_DATA.getOrPut(victim) {
            ExplodeStuckData(mutableListOf(), enchantLevel = level)
        }
        data.enchantLevel = maxOf(data.enchantLevel, level)

        val availableSlots = MAX_STACKS - data.arrows.size
        if (availableSlots <= 0) return

        val toAdd = minOf(level, availableSlots)
        for (i in 1..toAdd) {
            data.arrows.add(
                TimedArrow(
                    arrow.copy(),
                    currentTime + TIMEOUT_TICKS
                )
            )
        }

        if (data.arrows.size >= MAX_STACKS) {
            detonate(world, victim, data, data.arrows.size)
            return
        }

        updateIndicator(victim, data)
    }

    private fun detonate(world: ServerWorld, victim: LivingEntity, data: ExplodeStuckData, stacks: Int) {
        STUCK_DATA.remove(victim)
        data.indicator?.let { indicator ->
            ACTIVE_INDICATORS.remove(indicator.uuid)
            indicator.discard()
        }

        if (stacks <= 0) return

        val basePower = (data.enchantLevel + 1).toDouble()
        val missingStacks = (MAX_STACKS - stacks).coerceAtLeast(0)
        val factor = (1.0 - 0.2 * missingStacks).coerceAtLeast(0.0)
        val finalPower = (basePower * factor).toFloat()

        if (finalPower > 0f) {
            world.createExplosion(
                victim,
                victim.x,
                victim.y + (victim.height * 0.5),
                victim.z,
                finalPower,
                false,
                World.ExplosionSourceType.NONE
            )
        }
    }

    private fun updateIndicator(victim: LivingEntity, data: ExplodeStuckData) {
        val world = victim.world as? ServerWorld ?: return

        val count = data.arrows.size
        if (count <= 0) return

        val text = Text.literal("§6[§f$count/$MAX_STACKS§6]").append(Text.literal("💣").withColor(0xFF5555))
        val targetY = victim.y + victim.height + HEIGHT_OFFSET

        if (data.indicator == null || data.indicator!!.isRemoved) {
            val display = TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
            display.text = text
            display.billboardMode = BillboardMode.CENTER

            display.setPosition(victim.x, targetY, victim.z)
            display.addCommandTag(INDICATOR_TAG)

            ACTIVE_INDICATORS.add(display.uuid)

            world.spawnEntity(display)
            data.indicator = display
        } else {
            data.indicator?.text = text
            data.indicator?.setPosition(victim.x, targetY, victim.z)
        }
    }

    fun init() {
        ServerEntityEvents.ENTITY_LOAD.register { entity, _ ->
            if (entity is TextDisplayEntity && entity.commandTags.contains(INDICATOR_TAG)) {
                if (!ACTIVE_INDICATORS.contains(entity.uuid)) {
                    PENDING_REMOVALS.add(entity)
                }
            }
        }

        ServerEntityEvents.ENTITY_UNLOAD.register { entity, _ ->
            if (entity is LivingEntity && STUCK_DATA.containsKey(entity)) {
                val data = STUCK_DATA.remove(entity)
                data?.indicator?.let {
                    ACTIVE_INDICATORS.remove(it.uuid)
                    PENDING_REMOVALS.add(it)
                }
            }
        }

        ServerTickEvents.END_SERVER_TICK.register { _ ->
            if (PENDING_REMOVALS.isNotEmpty()) {
                PENDING_REMOVALS.forEach { it.discard() }
                PENDING_REMOVALS.clear()
            }

            val snapshot = STUCK_DATA.entries.toList()
            for ((victim, data) in snapshot) {
                if (!STUCK_DATA.containsKey(victim)) continue
                val world = victim.world as? ServerWorld ?: continue

                if (victim.isRemoved || !victim.isAlive || victim.health <= 0f) {
                    val stacksBeforeDeath = data.arrows.size
                    detonate(world, victim, data, stacksBeforeDeath)
                } else {
                    val currentTime = world.time

                    data.indicator?.let { indicator ->
                        if (!indicator.isRemoved) {
                            if (world.time % 2L == 0L) {
                                val targetY = victim.y + victim.height + HEIGHT_OFFSET
                                if (indicator.x != victim.x || indicator.y != targetY || indicator.z != victim.z) {
                                    indicator.setPosition(victim.x, targetY, victim.z)
                                }
                            }
                        }
                    }

                    val originalSize = data.arrows.size
                    data.arrows.removeAll { currentTime >= it.expireTime }
                    if (data.arrows.isEmpty()) {
                        STUCK_DATA.remove(victim)
                        data.indicator?.let { indicator ->
                            ACTIVE_INDICATORS.remove(indicator.uuid)
                            indicator.discard()
                        }
                    } else if (data.arrows.size != originalSize) {
                        updateIndicator(victim, data)
                    }
                }
            }
        }
    }
}