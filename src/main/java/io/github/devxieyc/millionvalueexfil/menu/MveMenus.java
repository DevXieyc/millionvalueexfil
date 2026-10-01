// MVE - menu registrations
package io.github.devxieyc.millionvalueexfil.menu;

import io.github.devxieyc.millionvalueexfil.Mve;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class MveMenus {
    private MveMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Mve.MODID);

    public static final Supplier<MenuType<PvpMenu>> PVP =
            MENUS.register("pvp", () -> IMenuTypeExtension.create(PvpMenu::new));
}
