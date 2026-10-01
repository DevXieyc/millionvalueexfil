// Million Value Exfil - placeholder death screen (bridges prepare packet to first view)
package io.github.devxieyc.millionvalueexfil.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MvePendingScreen extends Screen {
    public MvePendingScreen() {
        super(Component.translatable("mve.death.title"));
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        super.render(gfx, mouseX, mouseY, partialTick);
        gfx.pose().pushPose();
        gfx.pose().scale(2.0F, 2.0F, 1.0F);
        gfx.drawCenteredString(this.font, this.title, this.width / 4, 5, 0xFFFFFFFF);
        gfx.pose().popPose();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 1615855616, -1602211792);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
