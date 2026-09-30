// Million Value Exfil - game bus event wiring
package io.github.devxieyc.millionvalueexfil;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = Mve.MODID)
public final class TxEvents {
    private TxEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathFirst(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TxManager.onDeathEarly(player, event.getSource());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDeathLast(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TxManager.onDeathSettled(player, event.isCanceled());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TxManager.commit(player, event.getDrops(), event.isCanceled());
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TxManager.onRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TxManager.onLogin(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TxManager.onLogout(player);
        }
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer viewer)) return;
        if (event.getTarget() instanceof io.github.devxieyc.millionvalueexfil.entity.DebugCorpseEntity corpse) {
            if (TxManager.openPvpDummy(viewer, corpse)) event.setCanceled(true);
            return;
        }
        if (!(event.getTarget() instanceof ServerPlayer owner)) return;
        if (owner == viewer || !owner.isDeadOrDying()) return;
        if (TxManager.openPvp(viewer, owner)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        TxManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        TxManager.serverStopping = true;
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        TxManager.serverStopping = false;
    }
}
