package id.dragon.nagaclan.economy;

import java.util.UUID;

public interface EconomyGateway {
    EconomyResult withdraw(UUID playerId, double amount);
    EconomyResult deposit(UUID playerId, double amount);
    double balance(UUID playerId);
    String format(double amount);
    boolean available();
}
