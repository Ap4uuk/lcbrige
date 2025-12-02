package ru.ap4uuk.lcbridge.listeners;

import io.github.lightman314.lightmanscurrency.api.events.TradeEvent;
import io.github.lightman314.lightmanscurrency.api.events.WalletDropEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import ru.ap4uuk.essentialsx.api.EconomyAPI;
import ru.ap4uuk.essentialsx.api.EconomyTransactionContext;
import ru.ap4uuk.lcbridge.LCBridgeSyncGuard;
import ru.ap4uuk.lcbridge.LCEconomyAdapter;
import ru.ap4uuk.lcbridge.LightmansCurrencyBridgeMod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = LightmansCurrencyBridgeMod.MOD_ID)
public class LCLightmansListener {

    @SubscribeEvent
    public static void onCoinPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!LCEconomyAdapter.isCoin(event.getItem().getItem())) {
            return;
        }

        syncEssentialsBalance(player, "Pickup coin change");
    }

    @SubscribeEvent
    public static void onCoinToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        if (!LCEconomyAdapter.isCoin(event.getEntity().getItem())) {
            return;
        }

        syncEssentialsBalance(player, "Toss coin change");
    }

    @SubscribeEvent
    public static void onWalletDrop(WalletDropEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        syncEssentialsBalance(player, "Wallet updated");
    }

    @SubscribeEvent
    public static void onTradeComplete(TradeEvent.PostTradeEvent event) {
        var ref = event.getPlayerReference();
        var player = ref != null ? ref.getPlayer() : null;
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        syncEssentialsBalance(serverPlayer, "Trade completed");
    }

    private static void syncEssentialsBalance(ServerPlayer player, String reason) {
        if (LCBridgeSyncGuard.isSyncing()) {
            return;
        }

        try {
            LCBridgeSyncGuard.setSyncing(true);
            double lcBalance = LCEconomyAdapter.getTotalBalance(player);

            EconomyTransactionContext ctx = new EconomyTransactionContext(
                    player.getUUID(),
                    "LCBridge",
                    reason,
                    Map.of("time", String.valueOf(System.currentTimeMillis()))
            );

            EconomyAPI.get().setBalance(player.getUUID(), lcBalance, ctx);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LCBridgeSyncGuard.setSyncing(false);
        }
    }
}
