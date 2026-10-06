// MVE debug companion - full death pipeline observatory
package io.github.devxieyc.millionvalueexfil.debug;

import io.github.devxieyc.millionvalueexfil.Mve;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = MveDebug.MODID)
public final class RulerOfDeath {
    private RulerOfDeath() {}

    private static final Map<UUID, Integer> SEQ = new HashMap<>();

    // ---- damage: only log hits that would kill ----

    @SubscribeEvent
    public static void onLethalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getNewDamage() < player.getHealth()) return;
        Mve.LOGGER.info("[RULER] damage.lethal: player={} src={} health={} newDamage={}",
                player.getScoreboardName(), event.getSource().getMsgId(),
                player.getHealth(), event.getNewDamage());
    }

    @SubscribeEvent
    public static void onTotem(LivingUseTotemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Mve.LOGGER.info("[RULER] totem.used: player={} item={}",
                player.getScoreboardName(), event.getTotem().getItem());
    }

    // ---- death declaration: observed at all five priorities ----

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onDeathHighest(LivingDeathEvent event) {
        spyDeath("HIGHEST", event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onDeathHigh(LivingDeathEvent event) {
        spyDeath("HIGH", event);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public static void onDeathNormal(LivingDeathEvent event) {
        spyDeath("NORMAL", event);
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    public static void onDeathLow(LivingDeathEvent event) {
        spyDeath("LOW", event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeathLowest(LivingDeathEvent event) {
        spyDeath("LOWEST", event);
    }

    private static void spyDeath(String stage, LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        int seq = "HIGHEST".equals(stage)
                ? SEQ.merge(player.getUUID(), 1, Integer::sum)
                : SEQ.getOrDefault(player.getUUID(), 0);
        GameRules rules = player.serverLevel().getGameRules();
        Mve.LOGGER.info("[RULER] death[@{}]: seq={} player={} src={} canceled={} dead={} keepInv={} instantRespawn={} slots={}",
                stage, seq, player.getScoreboardName(), event.getSource().getMsgId(), event.isCanceled(),
                player.isDeadOrDying(), rules.getBoolean(GameRules.RULE_KEEPINVENTORY),
                rules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN), countNonEmpty(player));
    }

    // ---- drops: observed at all five priorities, pool size shows who consumed what ----

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onDropsHighest(LivingDropsEvent event) {
        spyDrops("HIGHEST", event);
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void onDropsHigh(LivingDropsEvent event) {
        spyDrops("HIGH", event);
    }

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public static void onDropsNormal(LivingDropsEvent event) {
        spyDrops("NORMAL", event);
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    public static void onDropsLow(LivingDropsEvent event) {
        spyDrops("LOW", event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDropsLowest(LivingDropsEvent event) {
        spyDrops("LOWEST", event);
    }

    private static void spyDrops(String stage, LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        StringBuilder items = new StringBuilder();
        int shown = 0;
        for (ItemEntity drop : event.getDrops()) {
            if (shown++ == 8) {
                items.append("…");
                break;
            }
            ItemStack stack = drop.getItem();
            items.append(stack.getItem()).append("x").append(stack.getCount()).append("; ");
        }
        Mve.LOGGER.info("[RULER] drops[@{}]: seq={} player={} canceled={} count={} [{}]",
                stage, SEQ.getOrDefault(player.getUUID(), 0), player.getScoreboardName(),
                event.isCanceled(), event.getDrops().size(), items);
    }

    // ---- experience ----

    @SubscribeEvent
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Mve.LOGGER.info("[RULER] xp.drop: seq={} player={} level={} dropped={} canceled={}",
                SEQ.getOrDefault(player.getUUID(), 0), player.getScoreboardName(),
                player.experienceLevel, event.getDroppedExperience(), event.isCanceled());
    }

    // ---- clone & respawn ----

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PlayerEvent.Clone clone = event;
        net.minecraft.world.entity.player.Player old = clone.getOriginal();
        Mve.LOGGER.info("[RULER] clone: seq={} player={} wasDeath={} oldSlots={} newSlots={} persistedKeys={}",
                SEQ.getOrDefault(player.getUUID(), 0), player.getScoreboardName(), clone.isWasDeath(),
                countNonEmpty(old), countNonEmpty(player),
                old.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG)
                        .getAllKeys());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Mve.LOGGER.info("[RULER] respawn: seq={} player={}",
                SEQ.getOrDefault(player.getUUID(), 0), player.getScoreboardName());
        SEQ.remove(player.getUUID());
    }

    private static int countNonEmpty(net.minecraft.world.entity.player.Player player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (!player.getInventory().getItem(slot).isEmpty()) count++;
        }
        return count;
    }
}
