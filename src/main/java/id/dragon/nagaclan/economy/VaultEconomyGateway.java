package id.dragon.nagaclan.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.UUID;

/** Optional Vault bridge. Reflection keeps Vault optional at compile and runtime. */
public final class VaultEconomyGateway implements EconomyGateway {
    private Object provider;
    private Method withdraw;
    private Method deposit;
    private Method balance;
    private Method format;

    public VaultEconomyGateway() {
        refresh();
    }

    @Override
    public EconomyResult withdraw(UUID playerId, double amount) {
        return transaction(withdraw, playerId, amount);
    }

    @Override
    public EconomyResult deposit(UUID playerId, double amount) {
        return transaction(deposit, playerId, amount);
    }

    @Override
    public double balance(UUID playerId) {
        if (!available()) return 0D;
        try {
            return ((Number) balance.invoke(provider, Bukkit.getOfflinePlayer(playerId))).doubleValue();
        } catch (ReflectiveOperationException exception) {
            refresh();
            return 0D;
        }
    }

    @Override
    public String format(double amount) {
        if (!available()) return String.format("%,.0f", amount);
        try {
            return String.valueOf(format.invoke(provider, amount));
        } catch (ReflectiveOperationException exception) {
            return String.format("%,.0f", amount);
        }
    }

    @Override
    public boolean available() {
        if (provider == null) refresh();
        return provider != null;
    }

    private EconomyResult transaction(Method method, UUID playerId, double amount) {
        if (amount <= 0D || !available() || method == null) return EconomyResult.failure("Vault economy tidak tersedia");
        try {
            Object response = method.invoke(provider, Bukkit.getOfflinePlayer(playerId), amount);
            Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
            if (Boolean.TRUE.equals(transactionSuccess.invoke(response))) return EconomyResult.success();
            Field errorMessage = response.getClass().getField("errorMessage");
            return EconomyResult.failure(String.valueOf(errorMessage.get(response)));
        } catch (ReflectiveOperationException exception) {
            refresh();
            return EconomyResult.failure("Transaksi ekonomi gagal");
        }
    }

    private void refresh() {
        provider = null;
        withdraw = deposit = balance = format = null;
        try {
            Class<?> economyType = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getRegistration(economyType);
            if (registration == null) return;
            Method providerMethod = registration.getClass().getMethod("getProvider");
            provider = providerMethod.invoke(registration);
            withdraw = economyType.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            deposit = economyType.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            balance = economyType.getMethod("getBalance", OfflinePlayer.class);
            format = economyType.getMethod("format", double.class);
        } catch (ReflectiveOperationException ignored) {
            provider = null;
        }
    }
}
