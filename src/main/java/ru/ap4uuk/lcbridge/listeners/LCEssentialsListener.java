package ru.ap4uuk.lcbridge.listeners;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import ru.ap4uuk.essentialsx.api.EconomyAPI;
import ru.ap4uuk.essentialsx.api.EconomyChangeEvent;
import ru.ap4uuk.essentialsx.api.EconomyChangeListener;
import ru.ap4uuk.lcbridge.LCBridgeSyncGuard;
import ru.ap4uuk.lcbridge.LCEconomyAdapter;

import java.util.UUID;

public class LCEssentialsListener implements EconomyChangeListener {

    public static void register() {
        try {
            EconomyAPI.registerListener(new LCEssentialsListener());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onEconomyChange(EconomyChangeEvent event) {
        // Если это изменение инициировали мы же (из LC) — пропускаем
        if (LCBridgeSyncGuard.isSyncing()) {
            return;
        }

        UUID uuid = event.playerId();
        double oldBalance = event.previousBalance();
        double newBalance = event.newBalance();
        double diff = newBalance - oldBalance;

        if (Math.abs(diff) < 0.01) {
            return;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player == null) {
            return;
        }

        try {
            LCBridgeSyncGuard.setSyncing(true);

            if (diff > 0) {
                // В Essentials прибавили денег → докидываем в LC
                LCEconomyAdapter.deposit(player, (long) diff);
            } else {
                // В Essentials списали → списываем в LC
                LCEconomyAdapter.withdraw(player, (long) Math.abs(diff));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            LCBridgeSyncGuard.setSyncing(false);
        }
    }
}
