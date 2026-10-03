// Million Value Exfil - PVP pickup screen (mod-owned chest texture, recessed item well)
package io.github.devxieyc.millionvalueexfil.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import io.github.devxieyc.millionvalueexfil.Mve;
import io.github.devxieyc.millionvalueexfil.menu.PvpPickupMenu;

public final class PvpPickupScreen extends AbstractContainerScreen<PvpPickupMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int FADE_TICKS = 200;
    private static final int YELLOW_TICKS = 600;

    public PvpPickupScreen(PvpPickupMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + PvpPickupMenu.PICKUP_ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        gfx.blit(BG, this.leftPos, this.topPos, 0, 0, this.imageWidth, PvpPickupMenu.PICKUP_ROWS * 18 + 17);
        gfx.blit(BG, this.leftPos, this.topPos + PvpPickupMenu.PICKUP_ROWS * 18 + 17, 0, 126, this.imageWidth, 96);

        int wellX = this.leftPos + 8;
        int wellY = this.topPos + 18;
        int wellW = 9 * 18;
        int wellH = PvpPickupMenu.PICKUP_ROWS * 18;
        gfx.fill(wellX, wellY, wellX + wellW, wellY + wellH, 0xFF0B0B0B);
        gfx.fill(wellX, wellY, wellX + wellW, wellY + 1, 0xFF000000);
        gfx.fill(wellX, wellY, wellX + 1, wellY + wellH, 0xFF000000);
        gfx.fill(wellX, wellY + wellH - 1, wellX + wellW, wellY + wellH, 0xFF555555);
        gfx.fill(wellX + wellW - 1, wellY, wellX + wellW, wellY + wellH, 0xFF555555);

        int itemCount = 0;
        for (int i = 0; i < PvpPickupMenu.PICKUP_SLOTS; i++) {
            if (!this.menu.pickupStack(i).isEmpty()) itemCount++;
        }
        int trackX = wellX + wellW - 4;
        int trackY = wellY + 1;
        int trackH = wellH - 2;
        gfx.fill(trackX, trackY, trackX + 4, trackY + trackH, 0xFF1A1A1A);
        int totalRows = (itemCount + PvpPickupMenu.PICKUP_COLUMNS - 1) / PvpPickupMenu.PICKUP_COLUMNS;
        if (totalRows > PvpPickupMenu.PICKUP_ROWS) {
            int thumbH = Math.max(8, trackH * PvpPickupMenu.PICKUP_ROWS / totalRows);
            gfx.fill(trackX, trackY, trackX + 4, trackY + thumbH, 0xFF8B8B8B);
        }

        Component timerComp = Component.translatable("mve.death.timer", timerText());
        int timerColor = this.menu.remainingTicks <= FADE_TICKS ? 0xFFFF5555
                : this.menu.remainingTicks <= YELLOW_TICKS ? 0xFFFFFF55 : 0xFFFFFFFF;
        gfx.drawString(this.font, timerComp, this.leftPos + this.imageWidth - this.font.width(timerComp) - 8,
                this.topPos + 6, timerColor);
    }

    private String timerText() {
        int total = Math.max(0, (this.menu.remainingTicks + 19) / 20);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    public void tickTimer() {
        if (this.menu.remainingTicks > 0) this.menu.remainingTicks--;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }
}
