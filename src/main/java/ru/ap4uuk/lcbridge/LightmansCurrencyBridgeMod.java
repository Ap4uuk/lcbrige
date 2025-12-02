package ru.ap4uuk.lcbridge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ru.ap4uuk.lcbridge.listeners.LCEssentialsListener;
import ru.ap4uuk.lcbridge.listeners.LCLoginSyncListener;

@Mod(LightmansCurrencyBridgeMod.MOD_ID)
public class LightmansCurrencyBridgeMod {

    public static final String MOD_ID = "lc_essentialsx_bridge";

    public LightmansCurrencyBridgeMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрируем Forge-ивент-лисенеры (логин и т.п.)
        MinecraftForge.EVENT_BUS.register(new LCLoginSyncListener());

        // Регистрируем слушатель экономики Essentials (через их API)
        LCEssentialsListener.register();
    }
}
