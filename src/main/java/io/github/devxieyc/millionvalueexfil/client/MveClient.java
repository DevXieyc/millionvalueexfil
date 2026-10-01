// Million Value Exfil - client payload handlers
package io.github.devxieyc.millionvalueexfil.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import io.github.devxieyc.millionvalueexfil.Mve;
import io.github.devxieyc.millionvalueexfil.MveNet;

public final class MveClient {
    private static final long PREPARE_TTL_MS = 5000;
    private static volatile long prepareStamp;

    private MveClient() {}

    public static void handlePrepare() {
        prepareStamp = System.currentTimeMillis();
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof MvePendingScreen)) {
            Mve.LOGGER.info("[MVE] prepare received, holding placeholder screen");
            mc.setScreen(new MvePendingScreen());
        }
    }

    public static boolean consumePrepare() {
        if (System.currentTimeMillis() - prepareStamp > PREPARE_TTL_MS) return false;
        prepareStamp = 0;
        return true;
    }

    public static void tickPrepareFallback() {
        long stamp = prepareStamp;
        if (stamp == 0 || System.currentTimeMillis() - stamp <= PREPARE_TTL_MS) return;
        prepareStamp = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MvePendingScreen || mc.screen == null) {
            if (mc.player != null && mc.player.isDeadOrDying()) {
                boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
                mc.setScreen(new DeathScreen(Component.empty(), hardcore));
            } else {
                mc.setScreen(null);
            }
        }
    }

    public static void handleView(MveNet.ViewPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.isDeadOrDying()) return;
        prepareStamp = 0;
        if (mc.screen instanceof MveDeathScreen screen && screen.matches(payload.txnId())) {
            screen.update(payload);
        } else {
            Mve.LOGGER.info("[MVE] opening death screen");
            mc.setScreen(new MveDeathScreen(payload));
        }
    }

    public static void handleClose(MveNet.ClosePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        prepareStamp = 0;
        if (mc.screen instanceof MvePendingScreen) {
            if (mc.player != null && mc.player.isDeadOrDying()) {
                boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
                mc.setScreen(new DeathScreen(Component.empty(), hardcore));
            } else {
                mc.setScreen(null);
            }
        }
    }

    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(
                io.github.devxieyc.millionvalueexfil.entity.MveEntities.DEBUG_CORPSE.get(),
                DebugCorpseRenderer::new);
    }
}
