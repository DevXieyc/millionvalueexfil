// MVE debug companion - death pipeline spy
package io.github.devxieyc.millionvalueexfil.debug;

import io.github.devxieyc.millionvalueexfil.Mve;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = MveDebug.MODID)
public final class DeathSpy {
    private DeathSpy() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathStart(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        GameRules rules = player.serverLevel().getGameRules();
        Mve.LOGGER.info("[MVE-SPY] death[HIGHEST]: player={} src={} mode={} keepInv={} instantRespawn={} nonEmptySlots={}",
                player.getScoreboardName(),
                event.getSource().getMsgId(),
                player.gameMode.getGameModeForPlayer(),
                rules.getBoolean(GameRules.RULE_KEEPINVENTORY),
                rules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN),
                countNonEmpty(player));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeathEnd(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Mve.LOGGER.info("[MVE-SPY] death[LOWEST]: player={} canceled={}",
                    player.getScoreboardName(), event.isCanceled());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        StringBuilder items = new StringBuilder();
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            items.append(stack.getItem()).append("x").append(stack.getCount()).append("; ");
        }
        Mve.LOGGER.info("[MVE-SPY] drops[LOWEST]: player={} canceled={} count={} [{}]",
                player.getScoreboardName(), event.isCanceled(), event.getDrops().size(), items);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Mve.LOGGER.info("[MVE-SPY] respawn: {}", event.getEntity().getScoreboardName());
    }

    private static int countNonEmpty(ServerPlayer player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (!player.getInventory().getItem(slot).isEmpty()) count++;
        }
        return count;
    }
}
