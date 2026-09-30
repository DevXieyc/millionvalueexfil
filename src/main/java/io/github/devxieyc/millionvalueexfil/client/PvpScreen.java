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
    private final GridView grid = new GridView(s -> s != 2);
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
        int cs = this.height >= 340 && this.width >= 400 ? 2 : 1;
        this.contentScale = cs;
        int cw = this.width / cs;
        int ch = this.height / cs;
        int rows = Mth.clamp((ch - 120) / 36, 1, 4);
        grid.layout(9, rows);

        gfx.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        int panelW = 9 * 18 + 14;
        int x0 = (cw - panelW) / 2;
        int y0 = Math.max(30 / cs, (ch - (24 + rows * 18) - 30) / 2);
        int invRows = 4;

        gfx.pose().pushPose();
        gfx.pose().scale(cs, cs, 1.0F);
        double mx = mouseX / (double) cs;
        double my = mouseY / (double) cs;

        drawPanel(gfx, x0, y0 - 3, panelW, 24 + rows * 18 + 6);
        tooltipEntry = grid.render(gfx, this.minecraft, x0 + 2, y0 + 9, mx, my);

        int yInv = y0 + 24 + rows * 18 + 12;
        gfx.drawString(this.font, Component.translatable("mve.pvp.inventory"), x0 + 2, yInv, 0xFFFFFF55);
        var inv = this.minecraft.player.getInventory();
        int idx = 0;
        for (int slot = 9; slot < 36; slot++) {
            drawVanillaSlot(gfx, inv.getItem(slot), x0 + (idx % 9) * 18, yInv + 12 + (idx / 9) * 18);
            idx++;
        }
        for (int slot = 0; slot < 9; slot++) {
            drawVanillaSlot(gfx, inv.getItem(slot), x0 + slot * 18, yInv + 12 + 3 * 18);
        }
        drawVanillaSlot(gfx, inv.getItem(40), x0 + 9 * 18 + 2, yInv + 12 + 3 * 18);

        gfx.pose().popPose();

        super.render(gfx, mouseX, mouseY, partialTick);

        if (tooltipEntry != null && !tooltipEntry.stack().isEmpty()) {
            gfx.renderTooltip(this.font, tooltipEntry.stack(), mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 1615855616, -1602211792);
    }

    private void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0x90060606);
        gfx.renderOutline(x, y, w, h, 0xFF555555);
    }

    private void drawVanillaSlot(GuiGraphics gfx, net.minecraft.world.item.ItemStack stack, int x, int y) {
        gfx.fill(x, y, x + 18, y + 18, 0xFF373737);
        gfx.renderOutline(x, y, 18, 18, 0xFF8B8B8B);
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
