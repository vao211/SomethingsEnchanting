package vao211.somethingsenchanting.enchantment

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.DisplayEntity.BillboardMode
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.PersistentProjectileEntity
import net.minecraft.item.ArrowItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import java.util.UUID
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class TimedArrow(val stack: ItemStack, val expireTime: Long)
data class StuckData(val arrows: MutableList<TimedArrow>, var indicator: TextDisplayEntity? = null)

object SplinterPinManager {
    private val STUCK_DATA = WeakHashMap<LivingEntity, StuckData>()
    private val ACTIVE_INDICATORS = mutableSetOf<UUID>()
    private val PENDING_REMOVALS = mutableSetOf<Entity>()
    private const val INDICATOR_TAG = "splinter_pin_indicator"
    private const val TIMEOUT_TICKS = 3600L
    private const val HEIGHT_OFFSET = 0.55
    private const val MAX_ARROWS = 20
    private const val SEARCH_RADIUS = 16.0

    fun addStuckArrow(victim: LivingEntity, arrow: ItemStack, level: Int) {
        if (victim.isRemoved || !victim.isAlive || victim.health <= 0f) return

        val world = victim.world as? ServerWorld ?: return
        val currentTime = world.time
        val safeLevel = level.coerceAtLeast(1)
        val data = STUCK_DATA.getOrPut(victim) { StuckData(mutableListOf()) }

        val availableSlots = MAX_ARROWS - data.arrows.size
        if (availableSlots <= 0) {
            releaseArrows(world, victim, data)
            return
        }

        val toAdd = minOf(safeLevel, availableSlots)
        for (i in 1..toAdd) {
            data.arrows.add(TimedArrow(arrow.copy(), currentTime + TIMEOUT_TICKS))
        }

        if (data.arrows.size >= MAX_ARROWS) {
            releaseArrows(world, victim, data)
            return
        }

        updateIndicator(victim, data)
    }

    private fun removeIndicator(data: StuckData) {
        data.indicator?.let { indicator ->
            ACTIVE_INDICATORS.remove(indicator.uuid)
            if (!indicator.isRemoved) {
                indicator.discard()
            }
        }
        data.indicator = null
    }

    private fun releaseArrows(world: ServerWorld, victim: LivingEntity, data: StuckData) {
        STUCK_DATA.remove(victim)
        removeIndicator(data)

        val arrowsToFire = data.arrows.toList()
        data.arrows.clear()
        if (arrowsToFire.isEmpty()) return

        val spawnX = victim.x
        val spawnY = victim.y + (victim.height * 0.4)
        val spawnZ = victim.z

        val nearbyTargets = world.getOtherEntities(
            victim,
            victim.boundingBox.expand(SEARCH_RADIUS)
        ) { entity ->
            entity is LivingEntity &&
                    entity !is PlayerEntity &&
                    entity.isAlive &&
                    !entity.isRemoved &&
                    entity.health > 0f &&
                    !entity.isSpectator
        }.map { it as LivingEntity }
            .sortedBy { it.squaredDistanceTo(spawnX, spawnY, spawnZ) }

        val dummyBow = ItemStack(Items.BOW)
        val totalArrows = arrowsToFire.size

        for ((index, timedArrow) in arrowsToFire.withIndex()) {
            val baseItem = timedArrow.stack.item
            val arrowStack = if (baseItem is ArrowItem) timedArrow.stack.copy() else ItemStack(Items.ARROW)
            val arrowItem = arrowStack.item as ArrowItem

            val projectile = arrowItem.createArrow(world, arrowStack, victim, dummyBow)
            projectile.setPosition(spawnX, spawnY, spawnZ)

            if (index < nearbyTargets.size) {
                val target = nearbyTargets[index]
                val targetX = target.x
                val targetY = target.y + (target.height * 0.55)
                val targetZ = target.z

                val dx = targetX - spawnX
                val dy = targetY - spawnY
                val dz = targetZ - spawnZ
                val horizontalDist = sqrt(dx * dx + dz * dz).coerceAtLeast(0.001)
                val compensatedDy = dy + (horizontalDist * 0.015)

                projectile.setVelocity(dx, compensatedDy, dz, 1.8f, 0.15f)
            } else {
                val randomIndex = index - nearbyTargets.size
                val baseAngle = (360.0 / totalArrows.coerceAtLeast(1)) * randomIndex + (world.random.nextDouble() * 45.0 - 22.5)
                val rad = Math.toRadians(baseAngle)
                val vy = 0.15 + world.random.nextDouble() * 0.25
                projectile.setVelocity(cos(rad), vy, sin(rad), 1.8f, 2.5f)
            }

            if (projectile is PersistentProjectileEntity) {
                projectile.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY
                projectile.damage = 5.0
            }

            world.spawnEntity(projectile)
        }

        world.playSound(null, victim.blockPos, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.5f)
    }

    private fun updateIndicator(victim: LivingEntity, data: StuckData) {
        val world = victim.world as? ServerWorld ?: return
        val count = data.arrows.size
        if (count <= 0) {
            removeIndicator(data)
            return
        }

        val text = Text.literal("§6[§f$count/$MAX_ARROWS§6]").append(Text.literal(" ➹").withColor(0xFF5555))
        val targetY = victim.y + victim.height + HEIGHT_OFFSET

        if (data.indicator == null || data.indicator!!.isRemoved || !data.indicator!!.isAlive) {
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
                data?.let { removeIndicator(it) }
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
                    releaseArrows(world, victim, data)
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
                        removeIndicator(data)
                    } else if (data.arrows.size != originalSize) {
                        updateIndicator(victim, data)
                    }
                }
            }
        }
    }
}