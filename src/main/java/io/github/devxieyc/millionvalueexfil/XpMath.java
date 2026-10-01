// Million Value Exfil - XP economy math
package io.github.devxieyc.millionvalueexfil;

import net.minecraft.util.Mth;

public final class XpMath {
    private XpMath() {}

    public static int cost(int n) {
        return 1395 * n * (n + 1) / 1332;
    }

    public static int marginal(int n) {
        return cost(n + 1) - cost(n);
    }

    public static int maxKeep(int xpTotal, int itemLimit) {
        int n = 0;
        while (n < itemLimit && cost(n + 1) <= xpTotal) n++;
        return n;
    }

    public static int levelForPoints(int points) {
        int lo = 0;
        int hi = 40000;
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (totalAt(mid) <= points) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    public static int deathDrop(int points) {
        return Math.min(7 * levelForPoints(points), 100);
    }

    public static long levelTotal(int level) {
        return totalAt(level);
    }

    public static float levelProgress(int points) {
        int level = levelForPoints(points);
        long current = totalAt(level);
        long next = totalAt(level + 1);
        if (next <= current) return 1.0F;
        return Mth.clamp((float) ((points - current) / (double) (next - current)), 0.0F, 1.0F);
    }

    private static long totalAt(int level) {
        if (level <= 16) return (long) level * level + 6L * level;
        if (level <= 31) return (long) (2.5 * level * level - 40.5 * level + 360);
        return (long) (4.5 * level * level - 162.5 * level + 2220);
    }
}
