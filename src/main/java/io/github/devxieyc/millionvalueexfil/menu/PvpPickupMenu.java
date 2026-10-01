// Million Value Exfil - PVP pickup menu (double-chest layout, pickup rows are read-only)
package io.github.devxieyc.millionvalueexfil.menu;

import io.github.devxieyc.millionvalueexfil.DeathTx;
import io.github.devxieyc.millionvalueexfil.TxEntry;
import io.github.devxieyc.millionvalueexfil.TxManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PvpPickupMenu extends AbstractContainerMenu {
    public static final int PICKUP_SLOTS = 54;
    public static final int PICKUP_ROWS = 6;

    private final net.minecraft.world.SimpleContainer pickup = new net.minecraft.world.SimpleContainer(PICKUP_SLOTS);
    private final List<UUID> slotEntries = new ArrayList<>(Collections.nCopies(PICKUP_SLOTS, (UUID) null));
    public final UUID txnId;
    public final UUID ownerId;

    public PvpPickupMenu(int id, Inventory playerInv, DeathTx txn) {
        super(MveMenus.PVP_PICKUP.get(), id);
        this.txnId = txn.txnId;
        this.ownerId = txn.ownerId;
        int slot = 0;
        for (TxEntry entry : txn.entries) {
            if (entry.state == TxEntry.State.DISABLED || slot >= PICKUP_SLOTS) continue;
            this.pickup.setItem(slot, entry.stack);
            this.slotEntries.set(slot, entry.id);
            slot++;
        }
        addSlots(playerInv);
    }

    public PvpPickupMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        super(MveMenus.PVP_PICKUP.get(), id);
        this.txnId = buf.readUUID();
        this.ownerId = buf.readUUID();
        for (int i = 0; i < PICKUP_SLOTS; i++) {
            if (buf.readBoolean()) {
                this.slotEntries.set(i, buf.readUUID());
                this.pickup.setItem(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
            }
        }
        addSlots(playerInv);
    }

    private void addSlots(Inventory playerInv) {
        for (int i = 0; i < PICKUP_SLOTS; i++) {
            int col = i % 9;
            int row = i / 9;
            addSlot(new Slot(this.pickup, i, 8 + col * 18, 18 + row * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, 9 + row * 9 + col, 8 + col * 18, 139 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 197));
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)
                && slotId >= 0 && slotId < PICKUP_SLOTS
                && this.getSlot(slotId).hasItem()
                && !player.level().isClientSide
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            UUID entryId = this.slotEntries.get(slotId);
            if (entryId != null) {
                TxManager.takeFromMenu(serverPlayer, this.txnId, this.ownerId, entryId);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public java.util.UUID slotEntry(int slot) {
        return this.slotEntries.get(slot);
    }

    public ItemStack pickupStack(int slot) {
        return this.pickup.getItem(slot);
    }

    public void removeEntry(UUID entryId) {
        for (int i = 0; i < PICKUP_SLOTS; i++) {
            if (entryId.equals(this.slotEntries.get(i))) {
                this.pickup.setItem(i, ItemStack.EMPTY);
                this.slotEntries.set(i, null);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
