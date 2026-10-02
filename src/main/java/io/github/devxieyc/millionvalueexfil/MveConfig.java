// Million Value Exfil - server config
package io.github.devxieyc.millionvalueexfil;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MveConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SELECTION_SECONDS;
    public static final ModConfigSpec.DoubleValue XP_KEEP_RATIO;
    public static final ModConfigSpec.BooleanValue ALLOW_HARDCORE;
    public static final ModConfigSpec.BooleanValue ALLOW_GENERIC_KILL;
    public static final ModConfigSpec.BooleanValue PVP_SHOW_OWN_INVENTORY;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SELECTION_SECONDS = b.comment("Selection time in seconds (t). Frozen while the server is offline.")
                .defineInRange("selectionSeconds", 90, 5, 3600);
        XP_KEEP_RATIO = b.comment("k in [0,1]: ratio of remaining XP kept after paying the retention cost.")
                .defineInRange("xpKeepRatio", 1.0, 0.0, 1.0);
        ALLOW_HARDCORE = b.comment("Allow MVE to trigger in hardcore worlds.")
                .define("allowHardcore", false);
        ALLOW_GENERIC_KILL = b.comment("Allow MVE to trigger on minecraft:generic_kill damage.")
                .define("allowGenericKill", false);
        PVP_SHOW_OWN_INVENTORY = b.comment("Show the viewer's own inventory section in the PVP pickup screen. "
                + "Disable for a pickup-only compat layout.")
                .define("pvpShowOwnInventory", true);
        SPEC = b.build();
    }

    private MveConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC);
    }
}
