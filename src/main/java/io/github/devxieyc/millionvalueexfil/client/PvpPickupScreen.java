// Million Value Exfil - PVP pickup screen (vanilla double-chest style)
package io.github.devxieyc.millionvalueexfil.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import io.github.devxieyc.millionvalueexfil.menu.PvpPickupMenu;

public final class PvpPickupScreen extends AbstractContainerScreen<PvpPickupMenu> {
    private static final ResourceLocation BG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public PvpPickupScreen(PvpPickupMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + PvpPickupMenu.PICKUP_ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        gfx.blit(BG, x, y, 0, 0, this.imageWidth, PvpPickupMenu.PICKUP_ROWS * 18 + 17);
        gfx.blit(BG, x, y + PvpPickupMenu.PICKUP_ROWS * 18 + 17, 0, 126, this.imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }
}
