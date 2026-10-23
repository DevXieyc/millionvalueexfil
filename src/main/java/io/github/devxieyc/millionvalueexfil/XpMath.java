// Million Value Exfil - XP economy math
package io.github.devxieyc.millionvalueexfil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import com.mojang.authlib.GameProfile;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * XP economy math. The level curve is NOT reimplemented here: every per-level
 * value comes from vanilla Player.getXpNeededForNextLevel() (NFD-verified,
 * 1.21.1: public, no-arg, reads this.experienceLevel) consulted through a
 * throwaway player bound to a world side. The curve is identical on both
 * sides, so a stale binding can only change which oracle was asked, not the
 * numbers it returns.
 */
public final class XpMath {
    private XpMath() {}

    /** Level whose vanilla curve the helpers consult; bound by the death screen. */
    private static volatile Level CONTEXT;

    public static void bind(Level level) {
        if (level != null) CONTEXT = level;
    }

    private static final class Curve {
        final Player oracle;
        final List<Integer> perLevel = new ArrayList<>();
        final List<Long> cumulative = new ArrayList<>(List.of(0L));

        Curve(Level level) {
            // Player is abstract only for isSpectator/isCreative (NFD-verified 1.21.1);
            // the curve itself lives in the concrete getXpNeededForNextLevel().
            oracle = new Player(level, level.getSharedSpawnPos(), 0.0F,
                    new GameProfile(UUID.randomUUID(), "mve-xp-curve")) {
                @Override public boolean isSpectator() { return false; }
                @Override public boolean isCreative() { return false; }
            };
        }

        int need(int lvl) {
            grow(lvl + 1);
            return perLevel.get(lvl);
        }

        long total(int lvl) {
            grow(lvl);
            return cumulative.get(lvl);
        }

        private void grow(int size) {
            while (perLevel.size() < size) {
                int l = perLevel.size();
                oracle.experienceLevel = l;
                int v = oracle.getXpNeededForNextLevel();
                perLevel.add(v);
                cumulative.add(cumulative.get(l) + v);
            }
        }
    }

    private static final Map<Level, Curve> CURVES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static Curve curve(Level level) {
        synchronized (CURVES) {
            return CURVES.computeIfAbsent(level, Curve::new);
        }
    }

    public static int cost(int n) {
        return 1395 * n * (n + 1) / 1332;
    }

    public static int maxKeep(int xpTotal, int itemLimit) {
        int n = 0;
        while (n < itemLimit && cost(n + 1) <= xpTotal) n++;
        return n;
    }

    public static int levelForPoints(int points) {
        Level level = CONTEXT;
        if (level == null) return 0;
        int lo = 0;
        int hi = 40000;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (curve(level).total(mid) <= points) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    public static float levelProgress(int points) {
        Level level = CONTEXT;
        if (level == null) return 0.0F;
        int lvl = levelForPoints(points);
        long current = curve(level).total(lvl);
        long next = curve(level).total(lvl + 1);
        if (next <= current) return 1.0F;
        return Mth.clamp((float) ((points - current) / (double) (next - current)), 0.0F, 1.0F);
    }

    /** Canonical points for a level+progress pair, derived from the vanilla curve. */
    public static int points(Level level, int lvl, float progress) {
        long base = curve(level).total(lvl);
        long span = curve(level).need(lvl);
        return (int) (base + Math.round(Mth.clamp(progress, 0.0F, 1.0F) * span));
    }
}
