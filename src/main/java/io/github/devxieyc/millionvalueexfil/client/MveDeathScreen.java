// Million Value Exfil - death selection screen
package io.github.devxieyc.millionvalueexfil.client;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import io.github.devxieyc.millionvalueexfil.MveNet;
import io.github.devxieyc.millionvalueexfil.XpMath;

public final class MveDeathScreen extends Screen {
    private static final ResourceLocation XP_BG = ResourceLocation.withDefaultNamespace("hud/experience_bar_background");
    private static final ResourceLocation XP_FILL = ResourceLocation.withDefaultNamespace("hud/experience_bar_progress");
    private static final int FADE_TICKS = 200;
    private static final int YELLOW_TICKS = 600;
    private static final int BAR_WIDTH = 9 * 18;
    private static final int PANEL_WIDTH = BAR_WIDTH + 20;

    private final UUID txnId;
    private MveNet.ViewPayload view;
    private int ticks;
    private final GridView kept = new GridView(s -> s == 1);
    private final GridView available = new GridView(s -> s != 1);
    private MveNet.EntryData tooltipEntry;
    private int contentScale = 1;
    private int gridRows = 2;
    private int keptGridY;
    private int availGridY;

    public MveDeathScreen(MveNet.ViewPayload view) {
        super(Component.translatable("mve.death.title"));
        this.view = view;
        this.txnId = view.txnId();
        this.ticks = view.ticksLeft();
        refreshGrids();
    }

    public boolean matches(UUID id) {
        return txnId.equals(id);
    }

    public void update(MveNet.ViewPayload view) {
        this.view = view;
        this.ticks = view.ticksLeft();
        refreshGrids();
    }

    private void refreshGrids() {
        kept.set(view.entries());
        available.set(view.entries());
    }

    @Override
    protected void init() {
        int bw = 150;
        addRenderableWidget(Button.builder(Component.translatable("mve.death.title_screen"), b -> confirmExit())
                .bounds(this.width / 2 - 155, this.height - 28, bw, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("mve.death.respawn"), b -> respawn())
                .bounds(this.width / 2 + 5, this.height - 28, bw, 20).build());
    }

    private void respawn() {
        if (this.minecraft != null && this.minecraft.player != null) this.minecraft.player.respawn();
    }

    private void confirmExit() {
        this.minecraft.setScreen(new ConfirmScreen(ok -> {
            if (ok) {
                if (this.minecraft.level != null) this.minecraft.level.disconnect();
                this.minecraft.disconnect(new net.minecraft.client.gui.screens.GenericMessageScreen(
                        Component.translatable("menu.savingLevel")));
                this.minecraft.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
            } else {
                this.minecraft.setScreen(this);
            }
        }, Component.translatable("mve.death.exit_title"), Component.translatable("mve.death.exit_msg"),
                Component.translatable("mve.death.to_title"), Component.translatable("mve.death.back")));
    }

    @Override
    public void tick() {
        super.tick();
        this.ticks = Math.max(0, this.ticks - 1);
        if (this.minecraft == null || this.minecraft.player == null || !this.minecraft.player.isDeadOrDying()) {
            this.onClose();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fillGradient(0, 0, this.width, this.height, 1615855616, -1602211792);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx, mouseX, mouseY, partialTick);
        int cs = this.height >= 340 && this.width >= 400 ? 2 : 1;
        this.contentScale = cs;

        gfx.pose().pushPose();
        gfx.pose().scale(2.0F, 2.0F, 1.0F);
        gfx.drawCenteredString(this.font, this.title, this.width / 4, 5, 0xFFFFFFFF);
        gfx.pose().popPose();
        gfx.drawCenteredString(this.font, view.deathMessage(), this.width / 2, 30, 0xFFFFFFFF);
        boolean showScore = this.height >= 300;
        int headerBottom = 32;
        if (showScore) {
            Component scoreText = Component.translatable("deathScreen.score.value",
                    Component.literal(String.valueOf(view.score())).withStyle(ChatFormatting.YELLOW));
            gfx.drawCenteredString(this.font, scoreText, this.width / 2, 42, 0xFFFFFFFF);
            headerBottom = 54;
        }

        int cw = this.width / cs;
        int availH = ch(cs) - Mth.ceil(headerBottom / (float) cs) - Mth.ceil(48f / cs);
        int rows = Mth.clamp((availH - 74) / 36, 2, 6);
        this.gridRows = rows;
        kept.layout(9, rows);
        available.layout(9, rows);

        int x0 = (cw - PANEL_WIDTH) / 2;
        int sectionH = 31 + rows * 18;
        int y0 = Mth.ceil(headerBottom / (float) cs) + 2;

        long keptCount = view.entries().stream().filter(e -> e.state() == 1).count();
        long maxKeep = Math.max(keptCount, maxKeep());
        int predictedPoints = keepEstimate();

        gfx.pose().pushPose();
        gfx.pose().scale(cs, cs, 1.0F);
        double mx = mouseX / (double) cs;
        double my = mouseY / (double) cs;

        drawPanel(gfx, x0, y0 - 3, PANEL_WIDTH, sectionH + 6);
        String capacity = "(" + keptCount + "/" + maxKeep + ")";
        gfx.drawString(this.font, Component.translatable("mve.death.kept"), x0 + 2, y0, 0xFFFFFFFF);
        gfx.drawString(this.font, Component.literal(capacity), x0 + PANEL_WIDTH - this.font.width(capacity) - 2, y0, 0xFFFFFFFF);
        xpBar(gfx, x0 + 2, y0 + 20, XpMath.levelProgress(predictedPoints));
        drawLevel(gfx, x0 + 2, y0 + 20, predictedPoints);
        this.keptGridY = y0 + 27;
        MveNet.EntryData keptHover = kept.render(gfx, this.minecraft, x0 + 2, this.keptGridY, mx, my);

        int availY = y0 + sectionH + 12;
        int availPanelY = availY - 3;
        drawPanel(gfx, x0, availPanelY, PANEL_WIDTH, sectionH + 6);
        Component timerComp = Component.translatable("mve.death.timer", timerText());
        int timerColor = this.ticks <= FADE_TICKS ? 0xFFFF5555 : this.ticks <= YELLOW_TICKS ? 0xFFFFFF55 : 0xFFFFFFFF;
        gfx.drawString(this.font, Component.translatable("mve.death.select"), x0 + 2, availY, 0xFFFFFFFF);
        gfx.drawString(this.font, timerComp, x0 + PANEL_WIDTH - this.font.width(timerComp) - 2, availY, timerColor);
        xpBar(gfx, x0 + 2, availY + 20, XpMath.levelProgress(view.xpTotal()));
        drawLevel(gfx, x0 + 2, availY + 20, view.xpTotal());
        this.availGridY = availY + 27;
        MveNet.EntryData availHover = available.render(gfx, this.minecraft, x0 + 2, this.availGridY, mx, my);
        if (this.ticks < FADE_TICKS) {
            float fade = 1.0F - this.ticks / (float) FADE_TICKS;
            int alpha = Math.min(210, (int) (fade * 150 + 55 * (float) Math.abs(Math.sin(this.ticks * 0.35))));
            gfx.fill(x0, availPanelY, x0 + PANEL_WIDTH, availPanelY + sectionH + 6, alpha << 24 | 0x060606);
        }
        this.tooltipEntry = availHover != null ? availHover : keptHover;

        gfx.pose().popPose();

        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(gfx, mouseX, mouseY, partialTick);
        }

        if (this.tooltipEntry != null && !this.tooltipEntry.stack().isEmpty()) {
            gfx.renderTooltip(this.font, this.tooltipEntry.stack(), mouseX, mouseY);
        }
    }

    private int ch(int scale) {
        return this.height / scale;
    }

    private void drawLevel(GuiGraphics gfx, int x, int barY, int points) {
        String text = String.valueOf(XpMath.levelForPoints(points));
        int tx = x + (BAR_WIDTH - this.font.width(text)) / 2;
        int ty = barY - 7;
        gfx.drawString(this.font, text, tx + 1, ty + 1, 0, false);
        gfx.drawString(this.font, text, tx, ty, 0x80FF20, false);
    }

    private void drawPanel(GuiGraphics gfx, int x, int y, int w, int h) {
        gfx.fill(x, y, x + w, y + h, 0x90060606);
        gfx.renderOutline(x, y, w, h, 0xFF555555);
    }

    private long maxKeep() {
        int choice = (int) view.entries().stream().filter(e -> e.state() != 2).count();
        int n = 0;
        while (n < choice && cost(n + 1) <= view.xpTotal()) n++;
        return n;
    }

    private static int cost(int n) {
        return 1395 * n * (n + 1) / 1332;
    }

    private int keepEstimate() {
        return view.xpRemain();
    }

    private String timerText() {
        int total = Math.max(0, (this.ticks + 19) / 20);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private void xpBar(GuiGraphics gfx, int x, int y, float ratio) {
        gfx.blitSprite(XP_BG, x, y, BAR_WIDTH, 5);
        int w = (int) (ratio * BAR_WIDTH);
        if (w > 0) gfx.blitSprite(XP_FILL, 182, 5, 0, 0, x, y, Math.min(w, BAR_WIDTH), 5);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != 0) return false;
        int cs = this.contentScale;
        double mx = mouseX / cs;
        double my = mouseY / cs;
        MveNet.EntryData inKept = kept.hit(mx, my);
        if (inKept != null) {
            MveNet.sendToServer(new MveNet.SelectPayload(txnId, view.version(), inKept.id(), false));
            return true;
        }
        if (this.ticks > 0) {
            MveNet.EntryData inAvail = available.hit(mx, my);
            if (inAvail != null) {
                MveNet.sendToServer(new MveNet.SelectPayload(txnId, view.version(), inAvail.id(), true));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) return true;
        double my = mouseY / this.contentScale;
        if (my >= this.availGridY && my < this.availGridY + this.gridRows * 18) available.scrollBy(scrollY);
        else if (my >= this.keptGridY && my < this.keptGridY + this.gridRows * 18) kept.scrollBy(scrollY);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
