// Million Value Exfil - client game bus events
package io.github.devxieyc.millionvalueexfil.client;

import net.minecraft.client.gui.screens.DeathScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = io.github.devxieyc.millionvalueexfil.Mve.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof DeathScreen && MveClient.consumePrepare()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MveClient.tickPrepareFallback();
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof PvpPickupScreen screen) screen.tickTimer();
    }

    @SubscribeEvent
    public static void onContainerClose(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Close event) {
        if (event.getContainer()
                instanceof io.github.devxieyc.millionvalueexfil.menu.PvpPickupMenu menu) {
            io.github.devxieyc.millionvalueexfil.TxManager.removeViewer(
                    event.getEntity().getUUID(), menu.ownerId);
        }
    }
}
