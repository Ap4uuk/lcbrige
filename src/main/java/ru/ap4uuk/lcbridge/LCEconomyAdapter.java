package ru.ap4uuk.lcbridge;

import io.github.lightman314.lightmanscurrency.api.money.MoneyAPI;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyValue;
import io.github.lightman314.lightmanscurrency.api.money.value.MoneyView;
import io.github.lightman314.lightmanscurrency.api.money.value.holder.IMoneyHolder;

import io.github.lightman314.lightmanscurrency.api.money.bank.BankAPI;
import io.github.lightman314.lightmanscurrency.api.money.bank.IBankAccount;
import io.github.lightman314.lightmanscurrency.api.money.bank.reference.BankReference;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class LCEconomyAdapter {

    /** Стоимость монет вручную под сайт-валюту */
    private static final LinkedHashMap<ResourceLocation, Long> COIN_VALUES = new LinkedHashMap<>();

    static {
        COIN_VALUES.put(id("lightmanscurrency:coin_netherite"), 100000L);
        COIN_VALUES.put(id("lightmanscurrency:coin_diamond"),   10000L);
        COIN_VALUES.put(id("lightmanscurrency:coin_emerald"),    1000L);
        COIN_VALUES.put(id("lightmanscurrency:coin_gold"),        100L);
        COIN_VALUES.put(id("lightmanscurrency:coin_iron"),         10L);
        COIN_VALUES.put(id("lightmanscurrency:coin_copper"),        1L);
    }

    /**
     * Коэффициент перевода валюты Essentials в минимальную валюту LC.
     * Минимальная единица — медная монета со стоимостью {@code 1}, поэтому 1 единица в Essentials
     * соответствует 1 медной монете по умолчанию. При необходимости можно изменить коэффициент,
     * чтобы учесть копейки или другую дробность в Essentials.
     */
    public static final BigDecimal ESSENTIALS_TO_LOWEST_COIN_RATE = BigDecimal.ONE;

    private static ResourceLocation id(String s) { return new ResourceLocation(s); }


    /* ============================================================
       1) МОНЕТЫ В ИНВЕНТАРЕ
       ============================================================ */

    public static long getInventoryBalance(ServerPlayer player) {
        long total = 0;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) continue;

            ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (id == null) continue;

            Long value = COIN_VALUES.get(id);
            if (value != null) {
                total += value * stack.getCount();
            }
        }

        return total;
    }


    /* ============================================================
       2) ВИРТУАЛЬНЫЙ БАЛАНС (MoneyAPI) — кошелёк в LC
       ============================================================ */

    public static long getWalletBalance(ServerPlayer player) {

        IMoneyHolder holder = MoneyAPI.getApi().GetPlayersMoneyHandler(player);
        if (holder == null)
            return 0;

        MoneyView view = holder.getStoredMoney();
        if (view == null || view.isEmpty())
            return 0;

        long total = 0;

        for (MoneyValue mv : view.allValues()) {
            if (mv != null && !mv.isEmpty())
                total += mv.getCoreValue();
        }

        return total;
    }


    /* ============================================================
       3) БАНКОВСКИЙ БАЛАНС LightmansCurrency
       ============================================================ */

    public static long getBankBalance(ServerPlayer player) {

        BankAPI api = BankAPI.getApi();
        long total = 0;

        List<BankReference> refs = api.GetAllBankReferences(false);

        for (BankReference ref : refs) {

            if (!ref.allowedAccess(player))
                continue;

            IBankAccount acc = ref.get();
            if (acc == null)
                continue;

            MoneyView mv = acc.getStoredMoney();
            if (mv == null || mv.isEmpty())
                continue;

            for (MoneyValue v : mv.allValues()) {
                if (v != null && !v.isEmpty())
                    total += v.getCoreValue();
            }
        }

        return total;
    }


    /* ============================================================
       4) ОБЩИЙ БАЛАНС
       ============================================================ */

    public static long getTotalBalance(ServerPlayer player) {
        return getInventoryBalance(player)
                + getWalletBalance(player)
                + getBankBalance(player);
    }


    /**
     * Переводит значение валюты Essentials в стоимость монет LC с округлением до ближайшей
     * минимальной монеты, чтобы не терять дробные значения из-за double.
     */
    public static long convertEssentialsToCoinValue(double essentialsAmount) {
        return BigDecimal.valueOf(essentialsAmount)
                .multiply(ESSENTIALS_TO_LOWEST_COIN_RATE)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }


    /* ============================================================
       5) ДЕПОЗИТ/СНЯТИЕ ЧЕРЕЗ КОНВЕРТАЦИЮ В МОНЕТЫ
       ============================================================ */

    public static void setTotalBalance(ServerPlayer player, long target) {
        long current = getTotalBalance(player);
        long diff = target - current;

        if (diff == 0)
            return;

        if (diff > 0)
            deposit(player, diff);
        else
            withdraw(player, -diff);
    }


    public static void deposit(ServerPlayer player, long amount) {

        for (Map.Entry<ResourceLocation, Long> e : COIN_VALUES.entrySet()) {
            ResourceLocation id = e.getKey();
            long coinValue = e.getValue();

            long count = amount / coinValue;
            if (count <= 0) continue;

            while (count > 0) {
                int stackSize = (int) Math.min(count, 64);

                ItemStack stack = makeItem(id, stackSize);
                if (!player.getInventory().add(stack))
                    player.drop(stack, false);

                count -= stackSize;
            }

            amount %= coinValue;
            if (amount <= 0)
                break;
        }
    }


    public static void withdraw(ServerPlayer player, long amount) {

        for (Map.Entry<ResourceLocation, Long> e : COIN_VALUES.entrySet()) {
            ResourceLocation id = e.getKey();
            long coinValue = e.getValue();

            long need = amount / coinValue;
            if (need <= 0) continue;

            long have = countItem(player, id);
            long take = Math.min(need, have);

            if (take > 0) {
                removeItems(player, id, take);
                amount -= take * coinValue;
            }

            if (amount <= 0) break;
        }
    }


    /* ============================================================
       УТИЛИТЫ
       ============================================================ */

    public static boolean isCoin(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return false;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && COIN_VALUES.containsKey(id);
    }

    private static ItemStack makeItem(ResourceLocation id, int count) {
        var item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }

    private static long countItem(ServerPlayer player, ResourceLocation id) {
        long sum = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.isEmpty()) continue;

            ResourceLocation sid = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (sid != null && sid.equals(id))
                sum += s.getCount();
        }
        return sum;
    }

    private static void removeItems(ServerPlayer player, ResourceLocation id, long count) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.items.size() && count > 0; i++) {
            ItemStack s = inv.items.get(i);
            if (s.isEmpty()) continue;

            ResourceLocation sid = ForgeRegistries.ITEMS.getKey(s.getItem());
            if (!id.equals(sid)) continue;

            int remove = (int) Math.min(count, s.getCount());
            s.shrink(remove);
            if (s.getCount() <= 0)
                inv.items.set(i, ItemStack.EMPTY);

            count -= remove;
        }
    }
}
