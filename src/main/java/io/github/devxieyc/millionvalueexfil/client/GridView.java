// Million Value Exfil - client slot grid widget
package io.github.devxieyc.millionvalueexfil.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import io.github.devxieyc.millionvalueexfil.MveNet;

final class GridView {
    private static final int STATE_DISABLED = 2;
    private static final int SLOT = 18;

    record EntryRect(MveNet.EntryData entry, int x, int y) {}

    private final List<MveNet.EntryData> entries = new ArrayList<>();
    private final List<EntryRect> rects = new ArrayList<>();
    private final IntPredicate stateFilter;
    private final boolean lightSlots;
    private int cols = 9;
    private int rows = 2;
    private int scroll;

    GridView(IntPredicate stateFilter) {
        this(stateFilter, false);
    }

    GridView(IntPredicate stateFilter, boolean lightSlots) {
        this.stateFilter = stateFilter;
        this.lightSlots = lightSlots;
    }

    void layout(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        scroll = Math.min(scroll, maxScroll());
    }

    void set(List<MveNet.EntryData> source) {
        entries.clear();
        for (MveNet.EntryData e : source) {
            if (stateFilter.test(e.state())) entries.add(e);
        }
        entries.sort(Comparator
                .comparingInt((MveNet.EntryData e) -> e.state() == STATE_DISABLED ? 1 : 0)
                .thenComparingInt(MveNet.EntryData::slot));
        scroll = Math.min(scroll, maxScroll());
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    int size() {
        return entries.size();
    }

    int maxScroll() {
        return Math.max(0, ((entries.size() + cols - 1) / cols) - rows);
    }

    void scrollBy(double amount) {
        int next = scroll - (int) Math.signum(amount);
        scroll = Math.max(0, Math.min(next, maxScroll()));
    }

    MveNet.EntryData render(GuiGraphics gfx, Minecraft mc, int x, int y, double mouseX, double mouseY) {
        rects.clear();
        gfx.enableScissor(x - 1, y - 1, x + cols * SLOT + 1, y + rows * SLOT + 1);
        MveNet.EntryData hovered = null;
        for (int i = scroll * cols; i < entries.size(); i++) {
            int idx = i - scroll * cols;
            int cx = x + (idx % cols) * SLOT;
            int cy = y + (idx / cols) * SLOT;
            if (cy >= y + rows * SLOT) break;
            MveNet.EntryData entry = entries.get(i);
            drawSlot(gfx, entry, cx, cy);
            rects.add(new EntryRect(entry, cx, cy));
            if (mouseX >= cx && mouseX < cx + SLOT && mouseY >= cy && mouseY < cy + SLOT) {
                gfx.renderOutline(cx, cy, SLOT, SLOT, 0xFFFFFFFF);
                if (entry.state() != STATE_DISABLED) hovered = entry;
            }
        }
        gfx.disableScissor();
        drawScrollbar(gfx, x, y);
        return hovered;
    }

    MveNet.EntryData hit(double mouseX, double mouseY) {
        for (EntryRect rect : rects) {
            if (mouseX >= rect.x() && mouseX < rect.x() + SLOT && mouseY >= rect.y() && mouseY < rect.y() + SLOT) {
                return rect.entry().state() == STATE_DISABLED ? null : rect.entry();
            }
        }
        return null;
    }

    private void drawSlot(GuiGraphics gfx, MveNet.EntryData entry, int x, int y) {
        if (lightSlots) {
            gfx.fill(x, y, x + SLOT, y + SLOT, 0x26FFFFFF);
        } else {
            gfx.fill(x, y, x + SLOT, y + SLOT, 0xFF373737);
        }
        gfx.renderOutline(x, y, SLOT, SLOT, 0xFF8B8B8B);
        if (!entry.stack().isEmpty()) {
            gfx.renderItem(entry.stack(), x + 1, y + 1);
            gfx.renderItemDecorations(Minecraft.getInstance().font, entry.stack(), x + 1, y + 1);
        }
        if (entry.state() == STATE_DISABLED) {
            gfx.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x90E03030);
        }
    }

    private void drawScrollbar(GuiGraphics gfx, int x, int y) {
        int trackX = x + cols * SLOT + 3;
        int trackH = rows * SLOT;
        int total = Math.max(1, (entries.size() + cols - 1) / cols);
        gfx.fill(trackX, y, trackX + 4, y + trackH, 0xFF1A1A1A);
        if (total > rows) {
            int thumbH = Math.max(8, trackH * rows / total);
            int thumbY = y + (trackH - thumbH) * scroll / (total - rows);
            gfx.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFF8B8B8B);
        }
    }
}
