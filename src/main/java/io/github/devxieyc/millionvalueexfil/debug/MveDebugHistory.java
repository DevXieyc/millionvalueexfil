// MVE debug companion - drop history archive with admin redrop
package io.github.devxieyc.millionvalueexfil.debug;

import io.github.devxieyc.millionvalueexfil.DeathTx;
import io.github.devxieyc.millionvalueexfil.Mve;
import io.github.devxieyc.millionvalueexfil.TxEntry;
import io.github.devxieyc.millionvalueexfil.TxManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class MveDebugHistory {
    private static final int MAX_RECORDS = 100;
    private static final int STALE_TICKS = 100;

    public static final class Record {
        public final UUID txnId;
        public final UUID ownerId;
        public final String ownerName;
        public final long time;
        public final ResourceKey<Level> dimension;
        public final long pos;
        public int phase;
        public boolean closed;
        public boolean redropped;
        public long updatedTick;
        public final List<Entry> entries = new ArrayList<>();

        Record(DeathTx txn, String name, long tick) {
            this(txn.txnId, txn.ownerId, name, System.currentTimeMillis(), txn.dimension, txn.pos.asLong(), txn.phase);
            this.updatedTick = tick;
            for (TxEntry e : txn.entries) {
                entries.add(new Entry(e.slot, e.state.ordinal(), e.stack));
            }
        }

        Record(UUID txnId, UUID ownerId, String name, long time, ResourceKey<Level> dimension, long pos, int phase) {
            this.txnId = txnId;
            this.ownerId = ownerId;
            this.ownerName = name;
            this.time = time;
            this.dimension = dimension;
            this.pos = pos;
            this.phase = phase;
        }

        public BlockPos blockPos() {
            return BlockPos.of(pos);
        }
    }

    public record Entry(int slot, int state, ItemStack stack) {}

    private static final Map<UUID, Record> RECORDS = new LinkedHashMap<>();
    private static Path file;

    private MveDebugHistory() {}

    public static void onServerStarted(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("mvedebug_history.nbt");
        load(server.registryAccess());
        Mve.LOGGER.info("[MVE-DEBUG] history archive loaded: {} record(s)", RECORDS.size());
    }

    public static void tick(MinecraftServer server) {
        if (file == null || server.getTickCount() % 20 != 0) return;
        boolean dirty = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            DeathTx txn = TxManager.readTxn(player);
            if (txn == null) continue;
            Record record = RECORDS.get(txn.txnId);
            if (record == null) {
                record = new Record(txn, player.getScoreboardName(), server.getTickCount());
                RECORDS.put(record.txnId, record);
                trim();
                Mve.LOGGER.info("[MVE-DEBUG] history recorded: {} for {} ({} item(s))",
                        record.txnId, record.ownerName, record.entries.size());
                dirty = true;
            }
            record.updatedTick = server.getTickCount();
            if (record.phase != txn.phase) {
                record.phase = txn.phase;
                dirty = true;
            }
            boolean entriesChanged = record.entries.size() != txn.entries.size();
            if (!entriesChanged) {
                for (int i = 0; i < txn.entries.size(); i++) {
                    TxEntry e = txn.entries.get(i);
                    Entry re = record.entries.get(i);
                    if (re.slot() != e.slot || re.state() != e.state.ordinal()
                            || !ItemStack.matches(re.stack(), e.stack)) {
                        entriesChanged = true;
                        break;
                    }
                }
            }
            if (entriesChanged) {
                record.entries.clear();
                for (TxEntry e : txn.entries) record.entries.add(new Entry(e.slot, e.state.ordinal(), e.stack));
                dirty = true;
            }
        }
        for (Record r : RECORDS.values()) {
            if (!r.closed && !r.redropped && server.getTickCount() - r.updatedTick > STALE_TICKS) {
                r.closed = true;
                dirty = true;
            }
        }
        if (dirty) save(server.registryAccess());
    }

    public static List<Record> all() {
        List<Record> list = new ArrayList<>(RECORDS.values());
        list.sort(Comparator.comparingLong((Record r) -> r.time).reversed());
        return list;
    }

    public static Record find(UUID txnId) {
        return RECORDS.get(txnId);
    }

    public static int redrop(MinecraftServer server, Record record) {
        ServerLevel level = server.getLevel(record.dimension);
        if (level == null) level = server.overworld();
        Random random = new Random();
        BlockPos pos = record.blockPos();
        int count = 0;
        for (Entry e : record.entries) {
            if (e.stack().isEmpty()) continue;
            ItemEntity entity = new ItemEntity(level,
                    pos.getX() + (random.nextDouble() - 0.5) * 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + (random.nextDouble() - 0.5) * 0.5,
                    e.stack().copy());
            entity.setPickUpDelay(40);
            level.addFreshEntity(entity);
            count++;
        }
        record.redropped = true;
        save(server.registryAccess());
        return count;
    }

    private static void trim() {
        Iterator<Map.Entry<UUID, Record>> it = RECORDS.entrySet().iterator();
        while (RECORDS.size() > MAX_RECORDS && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private static void save(HolderLookup.Provider registries) {
        if (file == null) return;
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (Record r : RECORDS.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("Id", r.txnId);
            t.putUUID("Owner", r.ownerId);
            t.putString("Name", r.ownerName);
            t.putLong("Time", r.time);
            t.putString("Dim", r.dimension.location().toString());
            t.putLong("Pos", r.pos);
            t.putInt("Phase", r.phase);
            t.putBoolean("Closed", r.closed);
            t.putBoolean("Redropped", r.redropped);
            ListTag entries = new ListTag();
            for (Entry e : r.entries) {
                CompoundTag et = new CompoundTag();
                et.putInt("Slot", e.slot());
                et.putInt("State", e.state());
                et.put("Stack", e.stack().save(registries));
                entries.add(et);
            }
            t.put("Entries", entries);
            list.add(t);
        }
        root.put("Records", list);
        try {
            NbtIo.writeCompressed(root, file);
        } catch (IOException e) {
            Mve.LOGGER.warn("[MVE-DEBUG] failed to save history: {}", e.toString());
        }
    }

    private static void load(HolderLookup.Provider registries) {
        RECORDS.clear();
        if (!java.nio.file.Files.isRegularFile(file)) return;
        CompoundTag root;
        try {
            root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            Mve.LOGGER.warn("[MVE-DEBUG] failed to load history: {}", e.toString());
            return;
        }
        ListTag list = root.getList("Records", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION,
                    ResourceLocation.parse(t.getString("Dim")));
            Record r = new Record(t.getUUID("Id"), t.getUUID("Owner"), t.getString("Name"),
                    t.getLong("Time"), dim, t.getLong("Pos"), t.getInt("Phase"));
            r.closed = t.getBoolean("Closed");
            r.redropped = t.getBoolean("Redropped");
            ListTag entries = t.getList("Entries", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int j = 0; j < entries.size(); j++) {
                CompoundTag et = entries.getCompound(j);
                ItemStack.parse(registries, et.getCompound("Stack"))
                        .ifPresent(stack -> r.entries.add(new Entry(et.getInt("Slot"), et.getInt("State"), stack)));
            }
            RECORDS.put(r.txnId, r);
        }
        trim();
    }
}
