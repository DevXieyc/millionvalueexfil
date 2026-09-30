// Million Value Exfil - transaction entry
package io.github.devxieyc.millionvalueexfil;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

public final class TxEntry {
    public enum State { DEFAULT, RESERVED, DISABLED }

    public final UUID id;
    public final int slot;
    public final ItemStack stack;
    public State state;

    public TxEntry(UUID id, int slot, ItemStack stack, State state) {
        this.id = id;
        this.slot = slot;
        this.stack = stack;
        this.state = state;
    }

    public CompoundTag write(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putInt("Slot", slot);
        tag.putString("State", state.name());
        tag.put("Item", stack.save(registries));
        return tag;
    }

    public static TxEntry read(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parseOptional(registries, tag.getCompound("Item"));
        State state;
        try {
            state = State.valueOf(tag.getString("State"));
        } catch (IllegalArgumentException e) {
            state = State.DISABLED;
        }
        return new TxEntry(tag.getUUID("Id"), tag.getInt("Slot"), stack, state);
    }

    public static ListTag writeList(java.util.List<TxEntry> entries, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (TxEntry e : entries) list.add(e.write(registries));
        return list;
    }

    public static java.util.List<TxEntry> readList(ListTag list, HolderLookup.Provider registries) {
        java.util.List<TxEntry> out = new java.util.ArrayList<>();
        for (Tag t : list) out.add(read((CompoundTag) t, registries));
        return out;
    }
}
