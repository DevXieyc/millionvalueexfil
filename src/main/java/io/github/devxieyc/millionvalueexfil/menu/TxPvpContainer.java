// MVE - server-side container view over a death transaction (copy-snapshot semantics)
package io.github.devxieyc.millionvalueexfil.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import io.github.devxieyc.millionvalueexfil.DeathTx;
import io.github.devxieyc.millionvalueexfil.TxEntry;

public class TxPvpContainer implements Container {
    private final DeathTx txn;
    private final List<UUID> slotIds;
    private final NonNullList<ItemStack> snapshot;
    private final Runnable onChange;

    public TxPvpContainer(DeathTx txn, Runnable onChange) {
        this.txn = txn;
        this.onChange = onChange;
        this.slotIds = new ArrayList<>();
        this.snapshot = NonNullList.withSize(txn.entries.size(), ItemStack.EMPTY);
        for (int i = 0; i < txn.entries.size(); i++) {
            TxEntry entry = txn.entries.get(i);
            slotIds.add(entry.id);
            this.snapshot.set(i, entry.stack.copy());
        }
    }

    public DeathTx txn() {
        return txn;
    }

    public void reconcile() {
        onChange.run();
    }

    private TxEntry entry(int slot) {
        if (slot < 0 || slot >= slotIds.size()) return null;
        return txn.find(slotIds.get(slot));
    }

    @Override
    public int getContainerSize() {
        return slotIds.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : snapshot) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.snapshot.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack stack = this.snapshot.get(slot);
        if (stack.isEmpty() || count <= 0) return ItemStack.EMPTY;
        ItemStack taken = stack.split(count);
        if (stack.isEmpty()) this.snapshot.set(slot, ItemStack.EMPTY);
        TxEntry entry = entry(slot);
        if (!taken.isEmpty() && entry != null) entry.stack.shrink(taken.getCount());
        if (!taken.isEmpty()) setChanged();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = this.snapshot.set(slot, ItemStack.EMPTY);
        TxEntry entry = entry(slot);
        if (!taken.isEmpty() && entry != null) entry.stack.setCount(0);
        if (!taken.isEmpty()) setChanged();
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.snapshot.set(slot, stack);
        TxEntry entry = entry(slot);
        if (entry != null) entry.stack.setCount(stack.getCount());
        setChanged();
    }

    @Override
    public void setChanged() {
        onChange.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.snapshot.size(); i++) {
            this.snapshot.set(i, ItemStack.EMPTY);
            TxEntry entry = entry(i);
            if (entry != null) entry.stack.setCount(0);
        }
        setChanged();
    }
}
