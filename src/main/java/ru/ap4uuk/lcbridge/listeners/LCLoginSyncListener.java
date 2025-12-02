package ru.ap4uuk.lcbridge.listeners;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.ap4uuk.essentialsx.api.EconomyAPI;
import ru.ap4uuk.essentialsx.api.EconomyTransactionContext;
import ru.ap4uuk.lcbridge.LCBridgeSyncGuard;
import ru.ap4uuk.lcbridge.LCEconomyAdapter;
import ru.ap4uuk.lcbridge.LightmansCurrencyBridgeMod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = LightmansCurrencyBridgeMod.MOD_ID)
public class LCLoginSyncListener {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        try {
            double lcBalance = LCEconomyAdapter.getTotalBalance(player);

            EconomyTransactionContext ctx = new EconomyTransactionContext(
                    player.getUUID(),
                    "LCBridge",
                    "Синхронизация с Lightman's Currency при входе",
                    Map.of("time", String.valueOf(System.currentTimeMillis()))
            );

            LCBridgeSyncGuard.setSyncing(true);
            EconomyAPI.get().setBalance(player.getUUID(), lcBalance, ctx);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LCBridgeSyncGuard.setSyncing(false);
        }
    }
}
