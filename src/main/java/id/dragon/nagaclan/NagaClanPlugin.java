package id.dragon.nagaclan;
import id.dragon.nagaclan.text.Messages;

import org.bukkit.plugin.java.JavaPlugin;
import id.dragon.nagaclan.command.ClanCommand;
import id.dragon.nagaclan.service.ClanService;
import id.dragon.nagaclan.command.AdminCommand;
import id.dragon.nagaclan.listener.ClanProtectionListener;
import id.dragon.nagaclan.gui.ClanMenuListener;
import id.dragon.nagaclan.gui.ClanMenuController;
import id.dragon.nagaclan.economy.VaultEconomyGateway;
import id.dragon.nagaclan.config.NagaClanConfig;
import id.dragon.nagaclan.limit.BukkitLuckPermsPermissionLookup;
import id.dragon.nagaclan.limit.ClanLimitService;
import id.dragon.nagaclan.service.ClanBankService;
import id.dragon.nagaclan.storage.YamlClanStorage;

public final class NagaClanPlugin extends JavaPlugin {
    private ClanMenuController menuController;
    private id.dragon.nagaclan.listener.ClanChatListener chatListener;
    private AutoCloseable placeholderHook;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        Messages.install(this);
        NagaClanConfig config = new NagaClanConfig(this);
        ClanLimitService limitService = new ClanLimitService(config.limits(),
                new BukkitLuckPermsPermissionLookup(message -> getLogger().warning(message)));
        YamlClanStorage clanStorage = new YamlClanStorage(getDataFolder());
        ClanService clanService = new ClanService(clanStorage, () -> java.util.Arrays.stream(org.bukkit.Material.values())
                .filter(material -> material.isItem() && !material.isAir() && !material.isLegacy())
                .map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()));
        clanService.restoreAll(clanStorage.load());
        ClanBankService bankService = new ClanBankService(new VaultEconomyGateway(), clanStorage);
        menuController = new ClanMenuController(this, clanService, bankService, limitService, config::menus);
        chatListener = new id.dragon.nagaclan.listener.ClanChatListener(this, clanService,
                new id.dragon.nagaclan.service.ClanChatService(clanService));
        ClanCommand clanCommand = new ClanCommand(this, clanService, limitService, menuController, chatListener);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("clan").setTabCompleter(clanCommand);
        var reloadService = new id.dragon.nagaclan.config.NagaClanReloadService(config, limitService,
                org.bukkit.Bukkit::isPrimaryThread, menuController::onReload);
        AdminCommand adminCommand = new AdminCommand(() -> getPluginMeta().getVersion(), clanService, reloadService);
        getCommand("nc").setExecutor(adminCommand);
        getCommand("nagaclan").setExecutor(adminCommand);
        getCommand("nc").setTabCompleter(adminCommand);
        getCommand("nagaclan").setTabCompleter(adminCommand);
        getServer().getPluginManager().registerEvents(new ClanMenuListener(menuController), this);
        getServer().getPluginManager().registerEvents(new ClanProtectionListener(clanService), this);
        getServer().getPluginManager().registerEvents(chatListener, this);
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                placeholderHook = new id.dragon.nagaclan.placeholder.PlaceholderHook(this, clanService);
                getLogger().info("PlaceholderAPI: placeholder NagaClan terdaftar.");
            } catch (RuntimeException | LinkageError failure) {
                getLogger().warning("Hook PlaceholderAPI tidak aktif: " + failure.getMessage());
            }
        }
        getLogger().info("NagaClan enabled.");
    }

    @Override public void onDisable() {
        if (placeholderHook != null) {
            try { placeholderHook.close(); }
            catch (Exception | LinkageError failure) { getLogger().warning("Gagal melepas hook PlaceholderAPI: " + failure.getMessage()); }
            placeholderHook = null;
        }
        if (chatListener != null) chatListener.close();
        if (menuController != null) menuController.close();
    }

}
