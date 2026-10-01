// Million Value Exfil - PVP pickup screen (pickup area rendered like the death selection grid)
package io.github.devxieyc.millionvalueexfil.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import io.github.devxieyc.millionvalueexfil.menu.PvpPickupMenu;

public final class PvpPickupScreen extends AbstractContainerScreen<PvpPickupMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int PANEL_FILL = 0x90060606;
    private static final int PANEL_OUTLINE = 0xFF555555;
    private static final int SLOT_FILL = 0xFF373737;
    private static final int SLOT_OUTLINE = 0xFF8B8B8B;
    private static final int TRACK = 0xFF1A1A1A;
    private static final int THUMB = 0xFF8B8B8B;

    public PvpPickupScreen(PvpPickupMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + PvpPickupMenu.PICKUP_ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    private int panelHeight() {
        return PvpPickupMenu.PICKUP_ROWS * 18 + 17;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.blit(BG, this.leftPos, this.topPos + PvpPickupMenu.PICKUP_ROWS * 18 + 17, 0, 126, this.imageWidth, 96);
        gfx.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + panelHeight(), PANEL_FILL);
        gfx.renderOutline(this.leftPos, this.topPos, this.imageWidth, panelHeight(), PANEL_OUTLINE);

        int itemCount = 0;
        for (int i = 0; i < PvpPickupMenu.PICKUP_SLOTS; i++) {
            if (!this.menu.pickupStack(i).isEmpty()) itemCount++;
        }
        int trackX = this.leftPos + this.imageWidth - 5;
        int trackY = this.topPos + 18;
        int trackH = PvpPickupMenu.PICKUP_ROWS * 18 - 2;
        gfx.fill(trackX, trackY, trackX + 4, trackY + trackH, TRACK);
        int totalRows = (itemCount + 8) / 9;
        if (totalRows > PvpPickupMenu.PICKUP_ROWS) {
            int thumbH = Math.max(8, trackH * PvpPickupMenu.PICKUP_ROWS / totalRows);
            gfx.fill(trackX, trackY, trackX + 4, trackY + thumbH, THUMB);
        }
    }

    @Override
    protected void renderSlot(GuiGraphics gfx, Slot slot) {
        if (this.menu.isPickupSlot(slot)) {
            gfx.fill(slot.x, slot.y, slot.x + 18, slot.y + 18, SLOT_FILL);
            gfx.renderOutline(slot.x, slot.y, 18, 18, SLOT_OUTLINE);
        }
        super.renderSlot(gfx, slot);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
        gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }
}
