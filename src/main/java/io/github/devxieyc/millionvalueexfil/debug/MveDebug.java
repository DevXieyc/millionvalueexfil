// MVE debug companion - in-game instrumentation
package io.github.devxieyc.millionvalueexfil.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.devxieyc.millionvalueexfil.DeathTx;
import io.github.devxieyc.millionvalueexfil.Mve;
import io.github.devxieyc.millionvalueexfil.TxManager;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(MveDebug.MODID)
public final class MveDebug {
    public static final String MODID = "mvedebug";

    public MveDebug(IEventBus modEventBus, ModContainer modContainer) {
        MveDebugEntities.TYPES.register(modEventBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(MveDebug::registerRenderers);
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MveDebug::registerCommands);
    }

    public static void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MveDebugEntities.DEBUG_CORPSE.get(), DebugCorpseRenderer::new);
    }

    private static void registerCommands(RegisterCommandsEvent event) {
        Mve.LOGGER.info("[MVE-SPY] debug commands registered");
        event.getDispatcher().register(Commands.literal("mvedebug")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("die").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    context.getSource().sendSuccess(
                            () -> Component.literal("[MVE-DEBUG] dealing fatal fall damage"), false);
                    player.hurt(player.damageSources().flyIntoWall(), Float.MAX_VALUE);
                    return 1;
                }))
                .then(Commands.literal("state").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    DeathTx txn = TxManager.readTxn(player);
                    String txnInfo = txn == null ? "none" : ("id=" + txn.txnId
                            + " entries=" + txn.entries.size()
                            + " reserved=" + txn.reservedCount()
                            + " ticks=" + txn.ticksLeft
                            + " version=" + txn.version);
                    String message = "[MVE-DEBUG] pending=" + TxManager.pendingCount()
                            + " txn=" + txnInfo
                            + " viewers=" + TxManager.viewerCount(player.getUUID())
                            + " xp=" + player.totalExperience + " (level " + player.experienceLevel + ")";
                    context.getSource().sendSuccess(() -> Component.literal(message), false);
                    return 1;
                }))
                .then(Commands.literal("corpse")
                        .executes(context -> spawnCorpse(context.getSource().getPlayerOrException(), 30))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1, 600))
                                .executes(context -> spawnCorpse(context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "seconds")))))
                .then(Commands.literal("xp")
                        .then(Commands.argument("points", IntegerArgumentType.integer())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int points = IntegerArgumentType.getInteger(context, "points");
                                    player.giveExperiencePoints(points);
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("[MVE-DEBUG] +" + points + " xp"), false);
                                    return 1;
                                })))
        );
    }

    private static int spawnCorpse(ServerPlayer player, int seconds) {
        DebugCorpseEntity corpse = MveDebugEntities.DEBUG_CORPSE.get().create(player.serverLevel());
        corpse.setPos(player.position());
        corpse.setYRot(player.getYRot());
        corpse.setLifeTicks(seconds * 20);
        corpse.setOwnerName(player.getScoreboardName());
        player.serverLevel().addFreshEntity(corpse);

        java.util.List<io.github.devxieyc.millionvalueexfil.TxEntry> entries = new java.util.ArrayList<>();
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            net.minecraft.world.item.ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()) {
                entries.add(new io.github.devxieyc.millionvalueexfil.TxEntry(
                        java.util.UUID.randomUUID(), slot, stack.copy(),
                        io.github.devxieyc.millionvalueexfil.TxEntry.State.DEFAULT));
            }
        }
        DeathTx txn = new DeathTx(java.util.UUID.randomUUID(), corpse.getUUID(), corpse.blockPosition(),
                player.level().dimension(), Component.literal("Debug corpse"), false, 0, 0,
                entries, 0, 0, DeathTx.PHASE_ACTIVE);
        TxManager.putDummyTxn(txn);
        int total = entries.size();
        player.sendSystemMessage(
                Component.literal("[MVE-DEBUG] corpse spawned with " + total + " item(s), expires in " + seconds + "s"));
        return 1;
    }
}
