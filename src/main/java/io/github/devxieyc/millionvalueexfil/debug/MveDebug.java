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
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MveDebug::onServerStarted);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(MveDebug::onServerTick);
    }

    private static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        MveDebugHistory.onServerStarted(event.getServer());
    }

    private static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        MveDebugHistory.tick(event.getServer());
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
                .then(Commands.literal("history")
                        .executes(context -> listHistory(context.getSource(), null))
                        .then(Commands.argument("player", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .executes(context -> listHistory(context.getSource(),
                                        com.mojang.brigadier.arguments.StringArgumentType.getString(context, "player")))))
                .then(Commands.literal("redrop")
                        .then(Commands.argument("txnId", net.minecraft.commands.arguments.UuidArgument.uuid())
                                .executes(context -> redrop(context.getSource(),
                                        net.minecraft.commands.arguments.UuidArgument.getUuid(context, "txnId")))))
        );
    }

    private static int listHistory(net.minecraft.commands.CommandSourceStack source, String filter) {
        java.util.List<MveDebugHistory.Record> records = MveDebugHistory.all();
        if (records.isEmpty()) {
            source.sendSuccess(() -> Component.literal("[MVE-DEBUG] no drop history"), false);
            return 1;
        }
        int shown = 0;
        for (MveDebugHistory.Record r : records) {
            if (filter != null && !r.ownerName.toLowerCase().contains(filter.toLowerCase())) continue;
            if (shown >= 20) break;
            String line = "[MVE-DEBUG] " + shortId(r.txnId) + " " + r.ownerName
                    + " " + phaseText(r) + " items=" + r.entries.size()
                    + " at " + r.blockPos().toShortString() + " " + r.dimension.location();
            source.sendSuccess(() -> Component.literal(line), false);
            shown++;
        }
        if (shown == 0) {
            source.sendSuccess(() -> Component.literal("[MVE-DEBUG] no history for " + filter), false);
        }
        return shown;
    }

    private static String shortId(java.util.UUID id) {
        return id.toString().substring(0, 8);
    }

    private static String phaseText(MveDebugHistory.Record r) {
        String phase = r.phase == DeathTx.PHASE_DROPPED ? "dropped" : "active";
        if (r.redropped) return phase + "/redropped";
        if (r.closed) return phase + "/closed";
        return phase;
    }

    private static int redrop(net.minecraft.commands.CommandSourceStack source, java.util.UUID txnId) {
        MveDebugHistory.Record record = MveDebugHistory.find(txnId);
        if (record == null) {
            source.sendFailure(Component.literal("[MVE-DEBUG] no such txn: " + txnId));
            return 0;
        }
        int count = MveDebugHistory.redrop(source.getServer(), record);
        source.sendSuccess(() -> Component.literal("[MVE-DEBUG] redropped " + count + " item(s) of "
                + record.ownerName + " at " + record.blockPos().toShortString()), false);
        return count;
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
