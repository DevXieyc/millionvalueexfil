// Million Value Exfil - transaction manager
package io.github.devxieyc.millionvalueexfil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import io.github.devxieyc.millionvalueexfil.menu.PvpMenu;
import io.github.devxieyc.millionvalueexfil.menu.TxPvpContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TxManager {
    public record PendingItem(int slot, ItemStack stack) {}

    public record Pending(List<PendingItem> items, int xpLevel, float xpProgress, int xpTotal,
                          BlockPos pos, ResourceKey<Level> dimension, Component message,
                          boolean hardcore, long stamp) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Set<UUID>> VIEWERS = new HashMap<>();
    private static final Map<UUID, DeathTx> DUMMY = new HashMap<>();
    private static final Map<UUID, TxPvpContainer> PVP_CONTAINERS = new HashMap<>();
    private static final String TXN_KEY = "Transaction";
    public static volatile boolean serverStopping = false;

    private TxManager() {}

    public static int pendingCount() {
        return PENDING.size();
    }

    public static int viewerCount(UUID ownerId) {
        Set<UUID> viewers = VIEWERS.get(ownerId);
        return viewers == null ? 0 : viewers.size();
    }

    // ---- persistence: transaction rides in the player's persisted NBT ----

    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(Player.PERSISTED_NBT_TAG)) data.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return data.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static void writeTxn(ServerPlayer player, DeathTx txn) {
        CompoundTag persisted = persisted(player);
        if (txn == null) persisted.remove(TXN_KEY);
        else persisted.put(TXN_KEY, txn.write(player.registryAccess()));
    }

    public static DeathTx readTxn(ServerPlayer player) {
        CompoundTag persisted = persisted(player);
        return persisted.contains(TXN_KEY)
                ? DeathTx.read(persisted.getCompound(TXN_KEY), player.registryAccess())
                : null;
    }

    private static DeathTx takeTxn(ServerPlayer player) {
        DeathTx txn = readTxn(player);
        if (txn != null) writeTxn(player, null);
        return txn;
    }

    // ---- death interception ----

    public static void onDeathEarly(ServerPlayer player, DamageSource source) {
        if (!shouldTrigger(player, source)) {
            Mve.LOGGER.info("[MVE] death skipped by trigger gate: {} / {}", player.getScoreboardName(), source);
            return;
        }
        List<PendingItem> items = new ArrayList<>();
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty()) items.add(new PendingItem(slot, stack.copy()));
        }
        if (items.isEmpty()) {
            Mve.LOGGER.info("[MVE] death skipped: empty inventory");
            return;
        }
        Mve.LOGGER.info("[MVE] death intercepted: {} item(s), xp level {}", items.size(), player.experienceLevel);
        Pending pending = new Pending(items, player.experienceLevel, player.experienceProgress,
                player.totalExperience, player.blockPosition(), player.level().dimension(),
                player.getCombatTracker().getDeathMessage(), player.level().getLevelData().isHardcore(),
                player.server.getTickCount());
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;
        PENDING.put(player.getUUID(), pending);
        MveNet.sendToPlayer(player, new MveNet.PreparePayload());
    }

    private static boolean shouldTrigger(ServerPlayer player, DamageSource source) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) return false;
        GameRules rules = player.serverLevel().getGameRules();
        if (rules.getBoolean(GameRules.RULE_KEEPINVENTORY)) return false;
        if (rules.getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN)) return false;
        if (player.level().getLevelData().isHardcore() && !MveConfig.ALLOW_HARDCORE.get()) return false;
        if (source.is(DamageTypes.GENERIC_KILL) && !MveConfig.ALLOW_GENERIC_KILL.get()) return false;
        return true;
    }

    public static void onDeathSettled(ServerPlayer player, boolean canceled) {
        if (!canceled) return;
        Pending pending = PENDING.remove(player.getUUID());
        if (pending != null) {
            Mve.LOGGER.info("[MVE] death averted by another mod, freezing undone");
            restoreXp(player, pending);
        }
    }

    private static void restoreXp(ServerPlayer player, Pending pending) {
        player.experienceLevel = pending.xpLevel();
        player.experienceProgress = pending.xpProgress();
        player.totalExperience = pending.xpTotal();
    }

    public static void commit(ServerPlayer player, java.util.Collection<ItemEntity> drops, boolean eventCanceled) {
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null) {
            Mve.LOGGER.info("[MVE] drops event without pending snapshot");
            return;
        }
        if (eventCanceled) {
            Mve.LOGGER.info("[MVE] drops event canceled by another mod");
            restoreXp(player, pending);
            MveNet.sendToPlayer(player, new MveNet.ClosePayload(player.getUUID()));
            return;
        }
        List<TxEntry> entries = new ArrayList<>();
        List<ItemEntity> pool = new ArrayList<>(drops);
        int matched = 0;
        for (PendingItem item : pending.items()) {
            ItemEntity hit = null;
            for (ItemEntity drop : pool) {
                if (ItemStack.matches(drop.getItem(), item.stack())) {
                    hit = drop;
                    break;
                }
            }
            if (hit != null) {
                pool.remove(hit);
                drops.remove(hit);
                entries.add(new TxEntry(UUID.randomUUID(), item.slot(), hit.getItem(), TxEntry.State.DEFAULT));
                matched++;
            } else {
                entries.add(new TxEntry(UUID.randomUUID(), item.slot(), item.stack(), TxEntry.State.DISABLED));
            }
        }
        if (matched == 0) {
            Mve.LOGGER.info("[MVE] commit aborted: no item survived into the drop flow ({} disabled)", entries.size());
            restoreXp(player, pending);
            MveNet.sendToPlayer(player, new MveNet.ClosePayload(player.getUUID()));
            return;
        }
        Mve.LOGGER.info("[MVE] transaction created: {} kept-custody + {} disabled, {} drop(s) left for others",
                matched, entries.size() - matched, drops.size());
        DeathTx txn = new DeathTx(UUID.randomUUID(), player.getUUID(), pending.pos(), pending.dimension(),
                pending.message(), pending.hardcore(), pending.xpTotal(), pending.xpLevel(), entries,
                MveConfig.SELECTION_SECONDS.get() * 20, 0, DeathTx.PHASE_ACTIVE);
        writeTxn(player, txn);
        broadcastView(player, txn, false);
    }

    // ---- per-tick countdown ----

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DeathTx txn = readTxn(player);
            if (txn == null || txn.phase != DeathTx.PHASE_ACTIVE) continue;
            txn.ticksLeft--;
            if (txn.ticksLeft <= 0) {
                txn.ticksLeft = 0;
                settle(player, txn);
            } else {
                writeTxn(player, txn);
                if (now % 20 == 0) broadcastView(player, txn, true);
            }
        }
        if (!PENDING.isEmpty()) {
            for (Map.Entry<UUID, Pending> e : List.copyOf(PENDING.entrySet())) {
                if (now - e.getValue().stamp() > 2) {
                    PENDING.remove(e.getKey());
                    ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
                    if (player != null) restoreXp(player, e.getValue());
                }
            }
        }
    }

    // ---- settlement ----

    private static void settle(ServerPlayer player, DeathTx txn) {
        ServerLevel level = player.server.getLevel(txn.dimension);
        if (level == null) level = player.serverLevel();
        int dropped = (int) txn.entries.stream().filter(e -> e.state == TxEntry.State.DEFAULT).count();
        dropDefaults(level, txn.pos, txn);
        txn.entries.removeIf(entry -> entry.state == TxEntry.State.DISABLED);
        int fee = txn.cost();
        int remain = Math.max(0, txn.xpTotal - fee);
        int keep = Math.min((int) Math.round(MveConfig.XP_KEEP_RATIO.get() * remain), remain);
        int lost = remain - keep;
        if (lost > 0) ExperienceOrb.award(level, Vec3.atCenterOf(txn.pos), XpMath.deathDrop(lost));
        player.giveExperiencePoints(keep);
        txn.phase = DeathTx.PHASE_DROPPED;
        txn.ticksLeft = 0;
        txn.version++;
        writeTxn(player, txn);
        Mve.LOGGER.info("[MVE] settled: {} item(s) kept for return, {} dropped at {}, xp keep={} lost-orbs={}",
                txn.reservedCount(), dropped, txn.pos.toShortString(), keep, lost > 0 ? XpMath.deathDrop(lost) : 0);
        broadcastView(player, txn, false);
        PVP_CONTAINERS.remove(txn.txnId);
        closeAll(player.server, txn.ownerId);
    }

    public static void onRespawn(ServerPlayer player) {
        DeathTx txn = takeTxn(player);
        if (txn == null) return;
        if (txn.phase == DeathTx.PHASE_ACTIVE) settle(player, txn);
        ServerLevel level = player.server.getLevel(txn.dimension);
        if (level == null) level = player.serverLevel();
        List<TxEntry> reserved = new ArrayList<>();
        for (TxEntry entry : txn.entries) if (entry.state == TxEntry.State.RESERVED) reserved.add(entry);
        reserved.sort(Comparator.comparingInt(e -> e.slot));
        for (TxEntry entry : reserved) {
            if (player.getInventory().getItem(entry.slot).isEmpty()) {
                player.getInventory().setItem(entry.slot, entry.stack);
            } else {
                player.getInventory().placeItemBackInInventory(entry.stack);
            }
        }
        Mve.LOGGER.info("[MVE] returned {} kept item(s) on respawn", reserved.size());
        PVP_CONTAINERS.remove(txn.txnId);
        closeAll(player.server, txn.ownerId);
    }

    public static void onLogout(ServerPlayer player) {
        UUID id = player.getUUID();
        PENDING.remove(id);
        VIEWERS.remove(id);
        for (Set<UUID> viewers : VIEWERS.values()) viewers.remove(id);
        if (serverStopping) return;
        DeathTx txn = takeTxn(player);
        if (txn == null) return;
        ServerLevel level = player.server.getLevel(txn.dimension);
        if (level == null) level = player.serverLevel();
        dropAll(level, txn);
        ExperienceOrb.award(level, Vec3.atCenterOf(txn.pos), Math.min(7 * txn.xpLevelAtDeath, 100));
        PVP_CONTAINERS.remove(txn.txnId);
        closeAll(player.server, txn.ownerId);
    }

    public static void onLogin(ServerPlayer player) {
        DeathTx txn = readTxn(player);
        if (txn != null) {
            sendView(player, txn, MveNet.ViewPayload.MODE_DEATH, false, player.getScore());
        }
    }

    private static void dropDefaults(ServerLevel level, BlockPos pos, DeathTx txn) {
        dropEntries(level, pos, txn, entry -> entry.state == TxEntry.State.DEFAULT);
    }

    private static void dropAll(ServerLevel level, DeathTx txn) {
        dropEntries(level, txn.pos, txn, entry -> entry.state != TxEntry.State.DISABLED);
        txn.entries.clear();
    }

    private interface EntryFilter {
        boolean test(TxEntry entry);
    }

    private static void dropEntries(ServerLevel level, BlockPos pos, DeathTx txn, EntryFilter filter) {
        if (level == null) return;
        RandomSource random = level.random;
        List<TxEntry> dropped = new ArrayList<>();
        for (TxEntry entry : txn.entries) {
            if (filter.test(entry)) {
                spawnDrop(level, pos, entry.stack, random);
                dropped.add(entry);
            }
        }
        txn.entries.removeAll(dropped);
        txn.version++;
    }

    private static void spawnDrop(ServerLevel level, BlockPos pos, ItemStack stack, RandomSource random) {
        ItemEntity entity = new ItemEntity(level,
                pos.getX() + (random.nextDouble() - 0.5) * 0.5,
                pos.getY() + 0.5,
                pos.getZ() + (random.nextDouble() - 0.5) * 0.5,
                stack);
        entity.setPickUpDelay(40);
        double angle = random.nextDouble() * Math.PI * 2;
        float speed = 0.15F + random.nextFloat() * 0.1F;
        entity.setDeltaMovement(Math.cos(angle) * speed, 0.25F, Math.sin(angle) * speed);
        level.addFreshEntity(entity);
    }

    // ---- client-server operations ----

    public static void handleSelect(ServerPlayer player, MveNet.SelectPayload payload) {
        DeathTx txn = readTxn(player);
        if (txn == null || txn.phase != DeathTx.PHASE_ACTIVE
                || !txn.txnId.equals(payload.txnId()) || txn.version != payload.version()) return;
        if (payload.reserve()) {
            if (!txn.toggle(payload.entryId())) return;
        } else {
            TxEntry entry = txn.find(payload.entryId());
            if (entry == null || entry.state != TxEntry.State.RESERVED) return;
            entry.state = TxEntry.State.DEFAULT;
            txn.version++;
        }
        player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, 1.6F);
        writeTxn(player, txn);
        broadcastView(player, txn, false);
    }

    public static boolean openPvp(ServerPlayer viewer, ServerPlayer owner) {
        DeathTx txn = readTxn(owner);
        if (txn == null) return false;
        return openPvpMenu(viewer, txn, owner, () -> writeTxn(owner, txn));
    }

    public static boolean openPvpDummy(ServerPlayer viewer, Entity corpse) {
        DeathTx txn = DUMMY.get(corpse.getUUID());
        if (txn == null) return false;
        return openPvpMenu(viewer, txn, null, () -> DUMMY.put(txn.ownerId, txn));
    }

    private static boolean openPvpMenu(ServerPlayer viewer, DeathTx txn, ServerPlayer owner, Runnable persist) {
        if (txn.phase != DeathTx.PHASE_ACTIVE || txn.entries.isEmpty()) return false;
        TxPvpContainer container = PVP_CONTAINERS.computeIfAbsent(txn.txnId,
                k -> new TxPvpContainer(txn, () -> reconcile(txn, owner, persist)));
        int rows = Mth.clamp((txn.entries.size() + 8) / 9, 1, 6);
        VIEWERS.computeIfAbsent(txn.ownerId, k -> new HashSet<>()).add(viewer.getUUID());
        viewer.openMenu(
                new net.minecraft.world.SimpleMenuProvider(
                        (cid, inv, p) -> new PvpMenu(cid, inv, container, rows),
                        Component.translatable("mve.pvp.title")),
                buf -> buf.writeVarInt(rows));
        return true;
    }

    private static void reconcile(DeathTx txn, ServerPlayer owner, Runnable persist) {
        boolean changed = txn.entries.removeIf(entry -> entry.stack.isEmpty());
        if (!changed) return;
        txn.version++;
        persist.run();
        if (owner != null) broadcastView(owner, txn, false);
    }

    public static void onPvpMenuClosed(ServerPlayer player, TxPvpContainer container) {
        DeathTx txn = container.txn();
        container.reconcile();
        Set<UUID> viewers = VIEWERS.get(txn.ownerId);
        if (viewers != null) viewers.remove(player.getUUID());
    }

    public static void putDummyTxn(DeathTx txn) {
        DUMMY.put(txn.ownerId, txn);
    }

    public static void expireDummy(UUID ownerId, ServerLevel level) {
        DeathTx txn = DUMMY.remove(ownerId);
        if (txn == null) return;
        PVP_CONTAINERS.remove(txn.txnId);
        for (TxEntry entry : txn.entries) {
            if (entry.state != TxEntry.State.DISABLED) spawnDrop(level, txn.pos, entry.stack, level.random);
        }
        txn.entries.clear();
        closeAll(level.getServer(), ownerId);
    }

    // ---- view sync ----

    private static int keepEstimate(DeathTx txn) {
        int remain = txn.xpRemain();
        return Math.min(remain, (int) Math.floor(MveConfig.XP_KEEP_RATIO.get() * remain));
    }

    private static MveNet.ViewPayload viewPayload(DeathTx txn, int mode, int score) {
        List<MveNet.EntryData> list = new ArrayList<>();
        for (TxEntry entry : txn.entries) {
            if (mode == MveNet.ViewPayload.MODE_PVP && entry.state == TxEntry.State.DISABLED) continue;
            list.add(new MveNet.EntryData(entry.id, entry.slot, entry.state.ordinal(), entry.stack));
        }
        return new MveNet.ViewPayload(txn.txnId, txn.ownerId, txn.version, mode, Math.max(0, txn.ticksLeft),
                txn.xpTotal, txn.cost(), keepEstimate(txn), txn.deathMessage, txn.hardcore, score, list);
    }

    private static void sendView(ServerPlayer to, DeathTx txn, int mode, boolean periodic, int score) {
        if (!periodic) {
            Mve.LOGGER.info("[MVE] sending view: mode={} to={}", mode == 0 ? "DEATH" : "PVP", to.getScoreboardName());
        }
        MveNet.sendToPlayer(to, viewPayload(txn, mode, score));
    }

    public static void broadcastView(ServerPlayer owner, DeathTx txn, boolean periodic) {
        broadcastView(owner, txn, periodic, owner);
    }

    public static void broadcastView(ServerPlayer owner, DeathTx txn, boolean periodic, Entity anchor) {
        int score = owner != null ? owner.getScore() : 0;
        ServerPlayer ownerPlayer = owner;
        if (ownerPlayer != null) {
            sendView(ownerPlayer, txn, MveNet.ViewPayload.MODE_DEATH, periodic, score);
        }
        Set<UUID> viewers = VIEWERS.get(txn.ownerId);
        if (viewers == null) return;
        for (UUID id : List.copyOf(viewers)) {
            ServerPlayer viewer = null;
            MinecraftServer server = ownerPlayer != null ? ownerPlayer.server : (anchor != null ? anchor.getServer() : null);
            if (server != null) viewer = server.getPlayerList().getPlayer(id);
            if (viewer != null) sendView(viewer, txn, MveNet.ViewPayload.MODE_PVP, periodic, score);
        }
    }

    public static void closeAll(MinecraftServer server, UUID ownerId) {
        Set<UUID> viewers = VIEWERS.remove(ownerId);
        if (viewers == null) return;
        MveNet.ClosePayload payload = new MveNet.ClosePayload(ownerId);
        for (UUID id : viewers) {
            ServerPlayer viewer = server.getPlayerList().getPlayer(id);
            if (viewer != null) MveNet.sendToPlayer(viewer, payload);
        }
    }
}
