package id.dragon.nagaclan.service;

import id.dragon.nagaclan.economy.EconomyGateway;
import id.dragon.nagaclan.economy.EconomyResult;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.model.ClanRole;
import id.dragon.nagaclan.storage.ClanStorage;

import java.io.IOException;
import java.util.UUID;

/** Applies Vault and clan-balance changes as one compensated operation. */
public final class ClanBankService {
    private final EconomyGateway economy;
    private final ClanStorage storage;

    public ClanBankService(EconomyGateway economy, ClanStorage storage) {
        this.economy = economy;
        this.storage = storage;
    }

    public BankResult deposit(UUID actor, Clan clan, long amount) {
        return deposit(actor, clan, amount, Long.MAX_VALUE);
    }

    public BankResult deposit(UUID actor, Clan clan, long amount, long maximumBank) {
        synchronized (clan) {
            if (!validAmount(amount)) return BankResult.INVALID_AMOUNT;
            if (!mayDeposit(actor, clan)) return BankResult.ACCESS_DENIED;
            if (!economy.available()) return BankResult.ECONOMY_UNAVAILABLE;
            long currentBank = clan.bank();
            if (maximumBank < currentBank || amount > maximumBank - currentBank) {
                return BankResult.INVALID_AMOUNT;
            }
            if (!economy.withdraw(actor, amount).successful()) return BankResult.ECONOMY_FAILURE;
            clan.setBank(currentBank + amount);
            try {
                storage.save(clan);
                return BankResult.SUCCESS;
            } catch (IOException exception) {
                EconomyResult refund = economy.deposit(actor, amount);
                if (refund.successful()) {
                    clan.setBank(currentBank);
                    return BankResult.STORAGE_FAILURE;
                }
                return persistCommittedTransaction(clan);
            }
        }
    }

    public BankResult withdraw(UUID actor, Clan clan, long amount) {
        if (!validAmount(amount)) return BankResult.INVALID_AMOUNT;
        if (!mayWithdraw(actor, clan)) return BankResult.ACCESS_DENIED;
        if (!economy.available()) return BankResult.ECONOMY_UNAVAILABLE;
        if (clan.bank() < amount) return BankResult.INVALID_AMOUNT;
        clan.setBank(clan.bank() - amount);
        if (!economy.deposit(actor, amount).successful()) {
            clan.setBank(clan.bank() + amount);
            return BankResult.ECONOMY_FAILURE;
        }
        try {
            storage.save(clan);
            return BankResult.SUCCESS;
        } catch (IOException exception) {
            EconomyResult reclaim = economy.withdraw(actor, amount);
            if (reclaim.successful()) {
                clan.setBank(clan.bank() + amount);
                return BankResult.STORAGE_FAILURE;
            }
            return persistCommittedTransaction(clan);
        }
    }

    public BankResult setMemberDepositAllowed(UUID actor, Clan clan, boolean allowed) {
        if (!isLeader(actor, clan)) return BankResult.ACCESS_DENIED;
        ClanRole previous = clan.settings().access(id.dragon.nagaclan.model.ClanAccess.DEPOSIT);
        clan.settings().setMemberDepositAllowed(allowed);
        return savePolicy(clan, previous, true);
    }

    public BankResult setMemberWithdrawAllowed(UUID actor, Clan clan, boolean allowed) {
        if (!isLeader(actor, clan)) return BankResult.ACCESS_DENIED;
        ClanRole previous = clan.settings().access(id.dragon.nagaclan.model.ClanAccess.WITHDRAW);
        clan.settings().setMemberWithdrawAllowed(allowed);
        return savePolicy(clan, previous, false);
    }

    public long playerBalance(UUID playerId) {
        return Math.max(0L, (long) Math.floor(economy.balance(playerId)));
    }

    public String format(long amount) {
        return economy.format(amount);
    }

    public boolean economyAvailable() {
        return economy.available();
    }

    private BankResult savePolicy(Clan clan, ClanRole previous, boolean depositPolicy) {
        try {
            storage.save(clan);
            return BankResult.SUCCESS;
        } catch (IOException exception) {
            clan.settings().setAccess(depositPolicy ? id.dragon.nagaclan.model.ClanAccess.DEPOSIT : id.dragon.nagaclan.model.ClanAccess.WITHDRAW, previous);
            return BankResult.STORAGE_FAILURE;
        }
    }

    private boolean mayDeposit(UUID actor, Clan clan) {
        return clan.settings().allows(clan.roleOf(actor), id.dragon.nagaclan.model.ClanAccess.DEPOSIT);
    }

    private boolean mayWithdraw(UUID actor, Clan clan) {
        return clan.settings().allows(clan.roleOf(actor), id.dragon.nagaclan.model.ClanAccess.WITHDRAW);
    }

    private boolean isLeader(UUID actor, Clan clan) {
        return clan.roleOf(actor) == ClanRole.LEADER;
    }

    private boolean validAmount(long amount) {
        return amount > 0L;
    }

    private BankResult persistCommittedTransaction(Clan clan) {
        try {
            storage.save(clan);
            return BankResult.SUCCESS;
        } catch (IOException retryFailure) {
            return BankResult.COMPENSATION_FAILURE;
        }
    }
}
