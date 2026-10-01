// Million Value Exfil - main mod entry
package io.github.devxieyc.millionvalueexfil;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Mve.MODID)
public final class Mve {
    public static final String MODID = "millionvalueexfil";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Mve(IEventBus modEventBus, ModContainer modContainer) {
        MveConfig.register(modContainer);
        modEventBus.addListener(MveNet::register);
        io.github.devxieyc.millionvalueexfil.entity.MveEntities.TYPES.register(modEventBus);
        io.github.devxieyc.millionvalueexfil.menu.MveMenus.MENUS.register(modEventBus);
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
            modEventBus.addListener(io.github.devxieyc.millionvalueexfil.client.MveClient::registerRenderers);
        }
    }
}
