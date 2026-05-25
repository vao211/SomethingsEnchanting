package vao211.somethingsenchanting.enchantment

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity
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

data class TimedArrow(val stack: ItemStack, val expireTime: Long)
data class StuckData(val arrows: MutableList<TimedArrow>, var indicator: TextDisplayEntity? = null)

object SplinterPinManager {
    private val STUCK_DATA = WeakHashMap<LivingEntity, StuckData>()
    private val ACTIVE_INDICATORS = mutableSetOf<UUID>()
    private val PENDING_REMOVALS = mutableSetOf<Entity>()
    private const val INDICATOR_TAG = "splinter_pin_indicator"
    private const val TIMEOUT_TICKS = 3600L

    fun addStuckArrow(victim: LivingEntity, arrow: ItemStack, level: Int) {
        if (victim.isRemoved) return

        val world = victim.world as? ServerWorld ?: return
        val currentTime = world.time
        val data = STUCK_DATA.getOrPut(victim) { StuckData(mutableListOf()) }

        for (i in 1..level) {
            data.arrows.add(TimedArrow(arrow.copy(), currentTime + TIMEOUT_TICKS))
        }

        if (victim.isAlive && victim.health > 0f) {
            updateIndicator(victim, data)
        }
    }

    private fun updateIndicator(victim: LivingEntity, data: StuckData) {
        val world = victim.world as? ServerWorld ?: return
        val count = data.arrows.size
        if (count <= 0) return

        val text = Text.literal("§6[§f$count§6]").append(Text.literal(" ➹").withColor(0xFF5555))

        if (data.indicator == null || data.indicator!!.isRemoved || !data.indicator!!.isAlive) {
            val display = TextDisplayEntity(EntityType.TEXT_DISPLAY, world)
            display.text = text
            display.setBillboardMode(net.minecraft.entity.decoration.DisplayEntity.BillboardMode.CENTER)
            display.setPosition(victim.x, victim.y + victim.height + 0.5, victim.z)
            display.addCommandTag(INDICATOR_TAG)

            ACTIVE_INDICATORS.add(display.uuid)

            display.startRiding(victim, true)
            world.spawnEntity(display)
            data.indicator = display
        } else {
            data.indicator?.text = text
            if (data.indicator!!.vehicle != victim) {
                data.indicator!!.startRiding(victim, true)
            }
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
                    it.stopRiding()
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
                    STUCK_DATA.remove(victim)
                    data.indicator?.let { indicator ->
                        ACTIVE_INDICATORS.remove(indicator.uuid)
                        indicator.stopRiding()
                        indicator.discard()
                    }

                    if (data.arrows.isNotEmpty()) {
                        val arrowsToFire = data.arrows
                        val totalArrows = arrowsToFire.size
                        val angleStep = 360.0 / totalArrows
                        var currentAngle = 0.0

                        for (timedArrow in arrowsToFire) {
                            val item = timedArrow.stack.item
                            if (item is ArrowItem) {
                                val dummyBow = ItemStack(Items.BOW)
                                val projectile = item.createArrow(world, timedArrow.stack, victim, dummyBow)

                                val rad = Math.toRadians(currentAngle)
                                projectile.setPosition(victim.x, victim.y + victim.height / 2.0, victim.z)
                                projectile.setVelocity(Math.cos(rad), 0.3, Math.sin(rad), 1.6f, 1.0f)

                                if (projectile is PersistentProjectileEntity) {
                                    projectile.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY
                                    projectile.damage = 5.0
                                }

                                world.spawnEntity(projectile)
                                currentAngle += angleStep
                            }
                        }
                        world.playSound(null, victim.blockPos, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.2f, 0.5f)
                    }
                }
                else {
                    val currentTime = world.time
                    val originalSize = data.arrows.size
                    data.arrows.removeAll { currentTime >= it.expireTime }
                    if (data.arrows.isEmpty()) {
                        STUCK_DATA.remove(victim)
                        data.indicator?.let { indicator ->
                            ACTIVE_INDICATORS.remove(indicator.uuid)
                            indicator.stopRiding()
                            indicator.discard()
                        }
                    }
                    else if (data.arrows.size != originalSize) {
                        updateIndicator(victim, data)
                    }
                }
            }
        }
    }
}