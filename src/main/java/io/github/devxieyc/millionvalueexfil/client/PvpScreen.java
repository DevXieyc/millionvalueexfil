// MVE - PVP pickup screen, vanilla chest-style container screen
package io.github.devxieyc.millionvalueexfil.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import io.github.devxieyc.millionvalueexfil.menu.PvpMenu;

public class PvpScreen extends AbstractContainerScreen<PvpMenu> {
    private static final ResourceLocation CONTAINER_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    public PvpScreen(PvpMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + menu.rowCount() * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int top = (this.height - this.imageHeight) / 2;
        gfx.blit(CONTAINER_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.menu.rowCount() * 18 + 17);
        gfx.blit(CONTAINER_TEXTURE, this.leftPos, this.topPos + this.menu.rowCount() * 18 + 17, 0, 126,
                this.imageWidth, 96);
    }
}
