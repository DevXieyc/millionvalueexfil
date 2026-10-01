// Million Value Exfil - PVP pickup screen
package io.github.devxieyc.millionvalueexfil.client;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import io.github.devxieyc.millionvalueexfil.MveNet;

public final class PvpScreen extends Screen {
    private final UUID ownerId;
    private MveNet.ViewPayload view;
    private final GridView grid = new GridView(s -> s != 2, true);
    private MveNet.EntryData tooltipEntry;
    private int contentScale = 1;

    public PvpScreen(MveNet.ViewPayload view) {
        super(Component.translatable("mve.pvp.title"));
        this.view = view;
        this.ownerId = view.ownerId();
        grid.set(view.entries());
    }

    public boolean matches(UUID ownerId) {
        return this.ownerId.equals(ownerId);
    }

    public void update(MveNet.ViewPayload view) {
        this.view = view;
        grid.set(view.entries());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        int cs = this.height >= 340 && this.width >= 400 ? 2 : 1;
        this.contentScale = cs;

        gfx.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        gfx.pose().pushPose();
        gfx.pose().scale(cs, cs, 1.0F);
        double mx = mouseX / (double) cs;
        double my = mouseY / (double) cs;

        int cw = this.width / cs;
        int ch = this.height / cs;
        int rows = Mth.clamp((ch - 130) / 36, 1, 4);
        grid.layout(9, rows);

        int gridX = (cw - (9 * 18 + 8)) / 2;
        int gridY = Mth.ceil(30f / cs) + 2;
        tooltipEntry = grid.render(gfx, this.minecraft, gridX, gridY, mx, my);

        int yInv = gridY + rows * 18 + 12;
        gfx.drawString(this.font, Component.translatable("mve.pvp.inventory"), gridX, yInv, 0xFFFFFFFF);
        var inv = this.minecraft.player.getInventory();
        int idx = 0;
        for (int slot = 9; slot < 36; slot++) {
            drawVanillaSlot(gfx, inv.getItem(slot), gridX + (idx % 9) * 18, yInv + 12 + (idx / 9) * 18);
            idx++;
        }
        for (int slot = 0; slot < 9; slot++) {
            drawVanillaSlot(gfx, inv.getItem(slot), gridX + slot * 18, yInv + 12 + 3 * 18);
        }
        drawVanillaSlot(gfx, inv.getItem(40), gridX + 9 * 18 + 2, yInv + 12 + 3 * 18);

        gfx.pose().popPose();

        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partialTick);
        }

        if (tooltipEntry != null && !tooltipEntry.stack().isEmpty()) {
            gfx.renderTooltip(this.font, tooltipEntry.stack(), mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xA0000000, 0xC0000000);
    }

    private void drawVanillaSlot(GuiGraphics gfx, net.minecraft.world.item.ItemStack stack, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, 0x66C8C8C8);
        gfx.renderOutline(x, y, 18, 18, 0xFF555555);
        if (!stack.isEmpty()) {
            gfx.renderItem(stack, x + 1, y + 1);
            gfx.renderItemDecorations(this.font, stack, x + 1, y + 1);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        MveNet.EntryData hit = grid.hit(mouseX / this.contentScale, mouseY / this.contentScale);
        if (hit == null) return false;
        MveNet.sendToServer(new MveNet.TakePayload(view.txnId(), view.ownerId(), view.version(), hit.id()));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        grid.scrollBy(scrollY);
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            MveNet.sendToServer(new MveNet.LeavePayload(ownerId));
        }
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
