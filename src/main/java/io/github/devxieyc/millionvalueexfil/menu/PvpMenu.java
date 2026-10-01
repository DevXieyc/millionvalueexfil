// MVE - chest-like menu over the pickup area; player inventory section is fully vanilla
package io.github.devxieyc.millionvalueexfil.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class PvpMenu extends AbstractContainerMenu {
    private final Container container;
    private final int rows;

    public PvpMenu(int id, Inventory playerInventory, Container container, int rows) {
        super(MveMenus.PVP.get(), id);
        AbstractContainerMenu.checkContainerSize(container, rows * 9);
        this.container = container;
        this.rows = rows;
        container.startOpen(playerInventory.player);
        int shift = (rows - 4) * 18;
        for (int j = 0; j < rows; j++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new PickupSlot(container, k + j * 9, 8 + k * 18, 18 + j * 18));
            }
        }
        for (int l = 0; l < 3; l++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, 103 + l * 18 + shift));
            }
        }
        for (int k = 0; k < 9; k++) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 161 + shift));
        }
    }

    public PvpMenu(int id, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf buf) {
        this(id, playerInventory, buf.readVarInt());
    }

    private PvpMenu(int id, Inventory playerInventory, int rows) {
        this(id, playerInventory, new SimpleContainer(9 * Mth.clamp(rows, 1, 6)), Mth.clamp(rows, 1, 6));
    }

    public int rowCount() {
        return this.rows;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index >= this.rows * 9) return ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!this.moveItemStackTo(stack, this.rows * 9, this.slots.size(), true)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
        if (this.container instanceof TxPvpContainer txContainer && player instanceof ServerPlayer serverPlayer) {
            io.github.devxieyc.millionvalueexfil.TxManager.onPvpMenuClosed(serverPlayer, txContainer);
        }
    }

    private static class PickupSlot extends Slot {
        PickupSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
