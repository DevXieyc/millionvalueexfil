// Million Value Exfil - death transaction
package io.github.devxieyc.millionvalueexfil;

import net.minecraft.nbt.NbtOps;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

public final class DeathTx {
    public static final int PHASE_ACTIVE = 0;
    public static final int PHASE_DROPPED = 1;

    public final UUID txnId;
    public final UUID ownerId;
    public final BlockPos pos;
    public final ResourceKey<Level> dimension;
    public final Component deathMessage;
    public final boolean hardcore;
    public final int xpTotal;
    public final int xpLevelAtDeath;
    public final List<TxEntry> entries;
    public int ticksLeft;
    public int version;
    public int phase;
    public final ResourceKey<DamageType> damageType;
    public int xpKeep;

    public DeathTx(UUID txnId, UUID ownerId, BlockPos pos, ResourceKey<Level> dimension,
                   Component deathMessage, boolean hardcore, int xpTotal, int xpLevelAtDeath,
                   List<TxEntry> entries, int ticksLeft, int version, int phase,
                   ResourceKey<DamageType> damageType) {
        this.txnId = txnId;
        this.ownerId = ownerId;
        this.pos = pos;
        this.dimension = dimension;
        this.deathMessage = deathMessage;
        this.hardcore = hardcore;
        this.xpTotal = xpTotal;
        this.xpLevelAtDeath = xpLevelAtDeath;
        this.entries = entries;
        this.ticksLeft = ticksLeft;
        this.version = version;
        this.phase = phase;
        this.damageType = damageType;
    }

    public int reservedCount() {
        int n = 0;
        for (TxEntry e : entries) if (e.state == TxEntry.State.RESERVED) n++;
        return n;
    }

    public int choiceCount() {
        int n = 0;
        for (TxEntry e : entries) if (e.state != TxEntry.State.DISABLED) n++;
        return n;
    }

    public int cost() {
        return XpMath.cost(reservedCount());
    }

    public int maxKeep() {
        return XpMath.maxKeep(xpTotal, choiceCount());
    }

    public int xpRemain() {
        return Math.max(0, xpTotal - cost());
    }

    public TxEntry find(UUID entryId) {
        for (TxEntry e : entries) if (e.id.equals(entryId)) return e;
        return null;
    }

    public void remove(UUID entryId) {
        entries.removeIf(e -> e.id.equals(entryId));
        version++;
    }

    public boolean toggle(UUID entryId) {
        TxEntry e = find(entryId);
        if (e == null || e.state == TxEntry.State.DISABLED) return false;
        if (e.state == TxEntry.State.DEFAULT) {
            if (reservedCount() >= maxKeep()) return false;
            e.state = TxEntry.State.RESERVED;
        } else {
            e.state = TxEntry.State.DEFAULT;
        }
        version++;
        return true;
    }

    public CompoundTag write(HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        root.putUUID("TxnId", txnId);
        root.putUUID("Owner", ownerId);
        root.putLong("Pos", pos.asLong());
        root.putString("Dim", dimension.location().toString());
        root.putInt("XpTotal", xpTotal);
        root.putInt("XpLevel0", xpLevelAtDeath);
        root.putInt("Ticks", ticksLeft);
        root.putInt("Version", version);
        root.putInt("Phase", phase);
        root.putBoolean("Hardcore", hardcore);
        if (damageType != null) root.putString("Dmg", damageType.location().toString());
        root.putInt("XpKeep", xpKeep);
        try {
            Tag msg = ComponentSerialization.CODEC
                    .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), deathMessage)
                    .getOrThrow();
            root.put("Msg", msg);
        } catch (Exception ignored) {
        }
        root.put("Entries", TxEntry.writeList(entries, registries));
        return root;
    }

    public static DeathTx read(CompoundTag root, HolderLookup.Provider registries) {
        Component msg = Component.empty();
        if (root.contains("Msg")) {
            try {
                msg = ComponentSerialization.CODEC
                        .parse(registries.createSerializationContext(NbtOps.INSTANCE), root.get("Msg"))
                        .getOrThrow();
            } catch (Exception ignored) {
            }
        }
        List<TxEntry> entries = root.contains("Entries")
                ? TxEntry.readList(root.getList("Entries", Tag.TAG_COMPOUND), registries)
                : new ArrayList<>();
        DeathTx txn = new DeathTx(
                root.getUUID("TxnId"),
                root.getUUID("Owner"),
                BlockPos.of(root.getLong("Pos")),
                ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(root.getString("Dim"))),
                msg,
                root.getBoolean("Hardcore"),
                root.getInt("XpTotal"),
                root.getInt("XpLevel0"),
                entries,
                root.getInt("Ticks"),
                root.getInt("Version"),
                root.getInt("Phase"),
                root.contains("Dmg")
                        ? ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(root.getString("Dmg")))
                        : null
        );
        txn.xpKeep = root.getInt("XpKeep");
        return txn;
    }
}
