// Million Value Exfil - PVP pickup compat screen (items-only page, no container menu)
package io.github.devxieyc.millionvalueexfil.client;

import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import io.github.devxieyc.millionvalueexfil.MveNet;

public final class PvpCompatScreen extends Screen {
    private static final int FADE_TICKS = 200;
    private static final int YELLOW_TICKS = 600;
    private static final int BAR_WIDTH = 9 * 18;
    private static final int PANEL_WIDTH = BAR_WIDTH + 20;

    private final UUID ownerId;
    private final UUID txnId;
    private MveNet.PvpOpenPayload view;
    private int ticks;
    private int contentScale = 1;
    private int gridRows = 2;
    private int gridY;
    private final GridView grid = new GridView(s -> s != 2);
    private MveNet.EntryData tooltipEntry;

    public PvpCompatScreen(MveNet.PvpOpenPayload payload) {
        super(Component.translatable("mve.pvp.title"));
        this.txnId = payload.txnId();
        this.ownerId = payload.ownerId();
        this.view = payload;
        this.ticks = payload.ticksLeft();
        refreshGrid();
    }

    public boolean matches(UUID txn, UUID owner) {
        return txnId.equals(txn) && ownerId.equals(owner);
    }

    public boolean matchesOwner(UUID owner) {
        return ownerId.equals(owner);
    }

    public void update(MveNet.PvpOpenPayload payload) {
        this.view = payload;
        this.ticks = payload.ticksLeft();
        refreshGrid();
    }

    private void refreshGrid() {
        grid.set(view.entries());
    }

    private void exit() {
        MveNet.sendToServer(new MveNet.LeavePayload(ownerId));
        if (this.minecraft != null) this.minecraft.setScreen(null);
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        int cs = this.height >= 340 && this.width >= 400 ? 2 : 1;
        this.contentScale = cs;

        long itemCount = view.entries().stream().filter(e -> e.state() != 2).count();
        int rows = Mth.clamp((int) ((itemCount + 8) / 9), 1, 6);
        this.gridRows = rows;
        grid.layout(9, rows);

        int x0 = (this.width / cs - PANEL_WIDTH) / 2;
        int headerH = 14;
        int panelH = headerH + rows * 18 + 7;
        int y0 = Math.max(8, (this.height / cs - panelH) / 2);

        gfx.pose().pushPose();
        gfx.pose().scale(cs, cs, 1.0F);
        double mx = mouseX / (double) cs;
        double my = mouseY / (double) cs;

        drawPanel(gfx, x0, y0, PANEL_WIDTH, panelH);
        gfx.drawString(this.font, this.title, x0 + 2, y0 + 3, 0xFFFFFFFF);
        Component timerComp = Component.translatable("mve.death.timer", timerText());
        int timerColor = this.ticks <= FADE_TICKS ? 0xFFFF5555 : this.ticks <= YELLOW_TICKS ? 0xFFFFFF55 : 0xFFFFFFFF;
        gfx.drawString(this.font, timerComp, x0 + PANEL_WIDTH - this.font.width(timerComp) - 2, y0 + 3, timerColor);
        this.gridY = y0 + headerH;
        this.tooltipEntry = grid.render(gfx, this.minecraft, x0 + 2, this.gridY, mx, my);

        gfx.pose().popPose();

        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partialTick);
        }

        if (this.tooltipEntry != null && !this.tooltipEntry.stack().isEmpty()) {
            gfx.renderTooltip(this.font, this.tooltipEntry.stack(), mouseX, mouseY);
        }
    }

    private void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0x90060606);
        gfx.renderOutline(x, y, w, h, 0xFF555555);
    }

    private String timerText() {
        int total = Math.max(0, (this.ticks + 19) / 20);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks = Math.max(0, this.ticks - 1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 69) {
            exit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        int cs = this.contentScale;
        double mx = mouseX / cs;
        double my = mouseY / cs;
        MveNet.EntryData hit = grid.hit(mx, my);
        if (hit != null) {
            MveNet.sendToServer(new MveNet.TakePayload(txnId, ownerId, hit.id()));
            return true;
        }
        exit();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        double my = mouseY / this.contentScale;
        if (my >= this.gridY && my < this.gridY + this.gridRows * 18) grid.scrollBy(scrollY);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
