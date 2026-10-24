package id.dragon.nagaclan.gui;
import id.dragon.nagaclan.text.Messages;

import id.dragon.nagaclan.config.MenuConfig;
import id.dragon.nagaclan.gui.page.*;
import id.dragon.nagaclan.limit.ClanLimitService;
import id.dragon.nagaclan.limit.ClanLimits;
import id.dragon.nagaclan.model.Clan;
import id.dragon.nagaclan.model.ClanRole;
import id.dragon.nagaclan.service.BankResult;
import id.dragon.nagaclan.service.ClanBankService;
import id.dragon.nagaclan.service.ClanService;
import id.dragon.nagaclan.text.SmallCaps;
import id.dragon.nagaclan.text.ClanColors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Holder-scoped GUI routing. Clan and role are rechecked immediately before every mutation. */
public final class ClanMenuController implements AutoCloseable {
    private final JavaPlugin plugin;
    private final ClanService clans;
    private final ClanBankService bank;
    private final ClanLimitService limits;
    private final Supplier<MenuConfig> menus;
    private final ChatPromptService prompts;
    private final MenuConfirmations confirmations = new MenuConfirmations();
    private final Map<UUID, UUID> dialogSessions = new HashMap<>();
    private final Map<UUID, Long> viewVersions = new HashMap<>();
    private long nextViewVersion;
    private boolean warnedDialogFailure;

    public ClanMenuController(JavaPlugin plugin, ClanService clans, ClanBankService bank,
                              ClanLimitService limits, Supplier<MenuConfig> menus) {
        this.plugin = Objects.requireNonNull(plugin);
        this.clans = Objects.requireNonNull(clans);
        this.bank = Objects.requireNonNull(bank);
        this.limits = Objects.requireNonNull(limits);
        this.menus = Objects.requireNonNull(menus);
        prompts = new ChatPromptService(plugin, clans, player -> open(player, ClanMenuPage.SETTINGS, 0));
        plugin.getServer().getPluginManager().registerEvents(prompts, plugin);
    }

    public void open(Player player, ClanMenuPage page, int number) {
        if (!plugin.isEnabled()) { player.closeInventory(); return; }
        if (number < 0) throw new IllegalArgumentException(Messages.text("Halaman tidak valid"));
        invalidateView(player.getUniqueId());
        confirmations.remove(player.getUniqueId());
        Clan clan = clans.clanOf(player.getUniqueId()).orElse(null);
        if (clan == null && page != ClanMenuPage.TOP) {
            show(player, null, OnboardingPage.render(menus.get()));
            return;
        }
        switch (page) {
            case MAIN, HOMES, BANK, DEPOSIT, WITHDRAW, BANK_SETTINGS ->
                    withLimits(player, (current, resolved) -> render(player, current, page, number, resolved));
            default -> render(player, clan, page, number, null);
        }
    }

    private void render(Player player, Clan clan, ClanMenuPage page, int number, ClanLimits resolved) {
        MenuConfig config = menus.get();
        ClanMenuRenderer renderer = new ClanMenuRenderer(config);
        MenuPageRenderer view = switch (page) {
            case MAIN -> new DashboardPage(renderer, config);
            case MEMBERS -> new MembersPage(renderer, config);
            case HOMES -> new HomesPage(renderer, config);
            case BANK, DEPOSIT, WITHDRAW, BANK_SETTINGS -> new BankPage(renderer, config, bank);
            case ALLIANCES -> new AlliancesPage(renderer, config, clans);
            case SETTINGS -> new SettingsPage(renderer, config);
            case ACCESS -> new AccessPage(renderer, config);
            case ICONS -> new IconPage(renderer, config);
            case TOP -> new TopPage(renderer, config, clans);
        };
        show(player, clan, view.render(player, clan, resolved, number));
    }

    private void show(Player player, Clan clan, Inventory inventory) {
        ((ClanMenuHolder) inventory.getHolder()).bindClan(clan);
        UUID playerId = player.getUniqueId();
        invalidateView(playerId);
        if (DialogSupport.available(plugin, player)) {
            UUID token = UUID.randomUUID();
            dialogSessions.put(playerId, token);
            ClanMenuHolder holder = (ClanMenuHolder) inventory.getHolder();
            try {
                player.closeInventory();
                NativeClanMenuDialog.show(player, clan, inventory,
                        action -> handleDialog(player, holder, token, action));
                return;
            } catch (RuntimeException | LinkageError failure) {
                dialogSessions.remove(playerId, token);
                if (!warnedDialogFailure) {
                    plugin.getLogger().warning("Clan dialog unavailable; falling back to inventory GUI: " + failure.getClass().getSimpleName());
                    warnedDialogFailure = true;
                }
            }
        }
        player.openInventory(inventory);
    }

    private void handleDialog(Player player, ClanMenuHolder holder, UUID token, MenuAction action) {
        runSync(() -> {
            UUID playerId = player.getUniqueId();
            if (!player.isOnline() || !plugin.isEnabled() || !dialogSessions.remove(playerId, token)) return;
            if (!holder.accepts(clans.clanOf(playerId).orElse(null), action)) {
                tell(player, "&#FFB86C" + Messages.text("Clan sudah berubah. Buka ulang menu."));
                return;
            }
            try { requirePermission(player, action.type()); handleNow(player, action); }
            catch (IllegalArgumentException | IllegalStateException failure) { tell(player, "&#FF5C8A" + failure.getMessage()); }
        });
    }

    private void invalidateView(UUID player) {
        viewVersions.put(player, ++nextViewVersion);
        dialogSessions.remove(player);
    }

    public void handle(Player player, MenuAction action) {
        var captured = player.getOpenInventory().getTopInventory().getHolder();
        if (!(captured instanceof ClanMenuHolder holder)) return;
        // Never open/close an inventory inside InventoryClickEvent itself.
        runSync(() -> {
            if (!player.isOnline() || player.getOpenInventory().getTopInventory().getHolder() != captured) return;
            if (!plugin.isEnabled()) { player.closeInventory(); return; }
            if (!holder.accepts(clans.clanOf(player.getUniqueId()).orElse(null), action)) {
                player.closeInventory();
                tell(player, ("&#FFB86C" + Messages.text("Clan sudah berubah. Buka ulang menu.")));
                return;
            }
            try { requirePermission(player, action.type()); handleNow(player, action); }
            catch (IllegalArgumentException | IllegalStateException failure) { tell(player, "&#FF5C8A" + failure.getMessage()); }
        });
    }

    private void handleNow(Player player, MenuAction action) {
        switch (action.type()) {
            case OPEN_DASHBOARD, BACK -> open(player, ClanMenuPage.MAIN, 0);
            case OPEN_MEMBERS -> open(player, ClanMenuPage.MEMBERS, action.page());
            case OPEN_MEMBER -> showMember(player, action);
            case OPEN_HOMES, HOMES -> open(player, ClanMenuPage.HOMES, action.page());
            case OPEN_BANK, BANK, DEPOSIT, WITHDRAW, BANK_SETTINGS -> open(player, ClanMenuPage.BANK, 0);
            case OPEN_ALLIANCES -> open(player, ClanMenuPage.ALLIANCES, action.page());
            case OPEN_SETTINGS, SETTINGS -> open(player, ClanMenuPage.SETTINGS, 0);
            case OPEN_ICONS -> open(player, ClanMenuPage.ICONS, action.page());
            case OPEN_ACCESS -> open(player, ClanMenuPage.ACCESS, action.page());
            case CYCLE_ACCESS -> {
                Clan clan = requireClan(player);
                var access = id.dragon.nagaclan.model.ClanAccess.valueOf(action.payload());
                ClanRole current = clan.settings().access(access);
                clans.setAccess(player.getUniqueId(), access, ClanRole.values()[(current.ordinal() + 1) % ClanRole.values().length]);
                open(player, ClanMenuPage.ACCESS, action.page());
            }
            case CUSTOM_ICON -> prompts.beginIcon(player, requireClan(player));
            case OPEN_TOP -> open(player, ClanMenuPage.TOP, action.page());
            case PREVIOUS_PAGE, NEXT_PAGE -> open(player, ClanMenuPage.valueOf(action.payload()), action.page());
            case HOME_TELEPORT -> teleportHome(player, action.payload());
            case BANK_DEPOSIT -> transact(player, action.payload(), true);
            case BANK_WITHDRAW -> transact(player, action.payload(), false);
            case TOGGLE_MEMBER_DEPOSIT, TOGGLE_MEMBER_WITHDRAW -> toggleBankAccess(player, action.type());
            case TOGGLE_FRIENDLY_FIRE -> {
                Clan clan = requireClan(player);
                clans.setFriendlyFire(player.getUniqueId(), !clan.settings().friendlyFire());
                open(player, ClanMenuPage.SETTINGS, 0);
            }
            case TOGGLE_TOP_VISIBILITY -> {
                Clan clan = requireClan(player);
                clans.setVisibleOnTop(player.getUniqueId(), !clan.settings().visibleOnTop());
                open(player, ClanMenuPage.SETTINGS, 0);
            }
            case EDIT_PREFIX -> prompts.beginPrefix(player, requireClan(player));
            case SELECT_ICON -> {
                clans.setIcon(player.getUniqueId(), action.payload());
                open(player, ClanMenuPage.ICONS, action.page());
            }
            case SET_BUILDER, SET_ADMIN -> {
                changeRole(player, action);
                open(player, ClanMenuPage.MEMBERS, action.page());
            }
            case MEMBER_KICK, SET_LEADER, LEAVE, DISBAND_CONFIRM -> askConfirmation(player, action);
            case CONFIRM_ACTION -> confirm(player);
            case CREATE_HELP -> {
                if (DialogSupport.available(plugin, player)) createFromDialog(player);
                else help(player, "/clan create <nama>");
            }
            case ACCEPT_HELP -> {
                if (DialogSupport.available(plugin, player)) {
                    Clan joined = clans.accept(player.getUniqueId());
                    tell(player, "&#4CC9F0" + Messages.text("Bergabung ke") + " " + joined.name());
                    open(player, ClanMenuPage.MAIN, 0);
                } else help(player, "/clan accept");
            }
            case DENY_HELP -> {
                if (DialogSupport.available(plugin, player)) {
                    clans.deny(player.getUniqueId());
                    tell(player, "&#FFB86C" + Messages.text("Undangan ditolak."));
                    open(player, ClanMenuPage.MAIN, 0);
                } else help(player, "/clan deny");
            }
            case CLOSE -> { forget(player.getUniqueId()); player.closeInventory(); }
            case NONE -> { }
            case DEPOSIT_AMOUNT, WITHDRAW_AMOUNT -> open(player, ClanMenuPage.BANK, 0);
        }
    }

    private void showMember(Player player, MenuAction action) {
        confirmations.remove(player.getUniqueId());
        Clan clan = requireClan(player);
        show(player, clan, MemberActionsPage.render(player, clan, UUID.fromString(action.payload()), action.page(), menus.get()));
    }

    private void validateMemberAction(Player player, Clan clan, MenuAction action) {
        UUID target = UUID.fromString(action.payload());
        ClanRole role = clan.roleOf(target);
        if (role == null || !RoleActionPolicy.actionsFor(clan, player.getUniqueId(), target).contains(action.type())) {
            throw new IllegalStateException(Messages.text("Role atau anggota sudah berubah"));
        }
    }

    private void changeRole(Player player, MenuAction action) {
        Clan clan = requireClan(player);
        validateMemberAction(player, clan, action);
        UUID oldLeader = clan.leaderId();
        UUID target = UUID.fromString(action.payload());
        ClanRole role = switch (action.type()) {
            case SET_LEADER -> ClanRole.LEADER;
            case SET_ADMIN -> ClanRole.ADMIN;
            default -> ClanRole.BUILDER;
        };
        clans.setRole(player.getUniqueId(), target, role);
        if (role == ClanRole.LEADER) {
            limits.invalidate(oldLeader);
            limits.invalidate(target);
        }
        tell(player, ("&#4CC9F0" + Messages.text("Role anggota disimpan.")));
    }

    private void askConfirmation(Player player, MenuAction requested) {
        Clan clan = requireClan(player);
        MenuAction action = requested;
        if (requested.type() == ClanMenuAction.LEAVE && clan.roleOf(player.getUniqueId()) == ClanRole.LEADER) {
            action = new MenuAction(ClanMenuAction.DISBAND_CONFIRM, "", 0);
        }
        if (action.type() == ClanMenuAction.MEMBER_KICK || action.type() == ClanMenuAction.SET_LEADER) {
            validateMemberAction(player, clan, action);
        }
        if (action.type() == ClanMenuAction.DISBAND_CONFIRM && clan.roleOf(player.getUniqueId()) != ClanRole.LEADER) {
            throw new IllegalStateException(Messages.text("Hanya leader dapat membubarkan clan"));
        }
        var renderer = new ClanMenuRenderer(menus.get());
        boolean memberAction = action.type() == ClanMenuAction.MEMBER_KICK || action.type() == ClanMenuAction.SET_LEADER;
        ClanMenuPage page = memberAction ? ClanMenuPage.MEMBERS : ClanMenuPage.MAIN;
        int size = menus.get().page(memberAction ? "members" : "main").size();
        int yes = size >= 27 ? 10 : 0, info = size >= 27 ? 13 : 1, no = size >= 27 ? 16 : 2;
        var menu = renderer.create(page, Map.of(
                yes, new MenuAction(ClanMenuAction.CONFIRM_ACTION, "", action.page()),
                no, new MenuAction(memberAction ? ClanMenuAction.OPEN_MEMBERS : ClanMenuAction.OPEN_DASHBOARD, "", action.page())));
        String label = switch (action.type()) {
            case MEMBER_KICK -> Messages.text("Keluarkan anggota?");
            case SET_LEADER -> Messages.text("Transfer leadership?");
            case DISBAND_CONFIRM -> Messages.text("Bubarkan seluruh clan?");
            default -> Messages.text("Keluar dari clan?");
        };
        String target = clan.name();
        if (memberAction) {
            UUID targetId = UUID.fromString(action.payload());
            target = player.getServer().getOfflinePlayer(targetId).getName();
            if (target == null) target = targetId.toString();
        }
        menu.setItem(info, renderer.item(Material.PAPER, "&#FFB86C&l" + label,
                List.of("&f" + target, Messages.style(("&7" + Messages.text("Konfirmasi berlaku 30 detik"))))));
        menu.setItem(yes, renderer.item(Material.LIME_DYE, ("&#4CC9F0" + Messages.text("Konfirmasi")), List.of()));
        menu.setItem(no, renderer.item(Material.RED_DYE, ("&#FF5C8A" + Messages.text("Batal")), List.of()));
        show(player, clan, menu);
        confirmations.begin(player.getUniqueId(), clan, action, Instant.now());
    }

    private void confirm(Player player) {
        Clan clan = requireClan(player);
        MenuAction action = confirmations.take(player.getUniqueId(), clan, Instant.now())
                .orElseThrow(() -> new IllegalStateException(Messages.text("Konfirmasi kedaluwarsa. Buka ulang menu.")));
        requirePermission(player, action.type());
        switch (action.type()) {
            case MEMBER_KICK -> {
                validateMemberAction(player, clan, action);
                clans.kick(player.getUniqueId(), UUID.fromString(action.payload()));
                open(player, ClanMenuPage.MEMBERS, action.page());
            }
            case SET_LEADER -> { changeRole(player, action); open(player, ClanMenuPage.MEMBERS, action.page()); }
            case LEAVE -> { clans.leave(player.getUniqueId()); open(player, ClanMenuPage.MAIN, 0); }
            case DISBAND_CONFIRM -> {
                UUID leader = clan.leaderId();
                clans.disband(player.getUniqueId());
                limits.invalidate(leader);
                open(player, ClanMenuPage.MAIN, 0);
            }
            default -> throw new IllegalStateException(Messages.text("Konfirmasi tidak valid"));
        }
        tell(player, ("&#4CC9F0" + Messages.text("Perubahan clan disimpan.")));
    }

    private void transact(Player player, String payload, boolean deposit) {
        long amount = Long.parseLong(payload);
        if (amount <= 0) throw new IllegalArgumentException(Messages.text("Nominal kosong. Saldo atau kapasitas bank tidak cukup."));
        if (deposit) {
            withLimits(player, (clan, resolved) -> {
                requirePermission(player, ClanMenuAction.BANK_DEPOSIT);
                long capacity = Math.min(bank.playerBalance(player.getUniqueId()), Math.max(0, resolved.bank() - clan.bank()));
                // Execute exactly the displayed amount. A changed balance rejects, never silently changes, the request.
                BankResult result = amount > capacity ? BankResult.INVALID_AMOUNT
                        : bank.deposit(player.getUniqueId(), clan, amount, resolved.bank());
                transactionResult(player, result);
            });
        } else transactionResult(player, bank.withdraw(player.getUniqueId(), requireClan(player), amount));
    }

    private void toggleBankAccess(Player player, ClanMenuAction type) {
        Clan clan = requireClan(player);
        BankResult result = type == ClanMenuAction.TOGGLE_MEMBER_DEPOSIT
                ? bank.setMemberDepositAllowed(player.getUniqueId(), clan, !clan.settings().memberDepositAllowed())
                : bank.setMemberWithdrawAllowed(player.getUniqueId(), clan, !clan.settings().memberWithdrawAllowed());
        transactionResult(player, result);
    }

    private void transactionResult(Player player, BankResult result) {
        tell(player, switch (result) {
            case SUCCESS -> ("&#4CC9F0" + Messages.text("Perubahan bank berhasil."));
            case ACCESS_DENIED -> ("&#FF5C8A" + Messages.text("Tidak memiliki akses bank clan."));
            case INVALID_AMOUNT -> ("&#FFB86C" + Messages.text("Saldo atau kapasitas berubah. Pilih ulang nominal."));
            case ECONOMY_UNAVAILABLE -> ("&#FFB86C" + Messages.text("Vault atau provider economy belum tersedia."));
            case ECONOMY_FAILURE -> ("&#FF5C8A" + Messages.text("Transaksi economy gagal."));
            case STORAGE_FAILURE -> ("&#FF5C8A" + Messages.text("Penyimpanan gagal. Transaksi dibatalkan."));
            case COMPENSATION_FAILURE -> ("&#FF5C8A" + Messages.text("Hubungi admin untuk memeriksa transaksi. Jangan ulangi."));
        });
        if (result == BankResult.COMPENSATION_FAILURE) player.closeInventory();
        else open(player, ClanMenuPage.BANK, 0);
    }

    private void withLimits(Player player, BiConsumer<Clan, ClanLimits> action) {
        Clan captured = requireClan(player);
        UUID leader = captured.leaderId();
        long capturedGeneration = limits.generation();
        long capturedView = viewVersions.getOrDefault(player.getUniqueId(), -1L);
        Inventory origin = player.getOpenInventory().getTopInventory();
        limits.resolve(captured).whenComplete((resolved, failure) -> runSync(() -> {
            if (!player.isOnline() || !plugin.isEnabled()
                    || capturedView != viewVersions.getOrDefault(player.getUniqueId(), -1L)
                    || player.getOpenInventory().getTopInventory() != origin) return;
            Clan current = clans.clanOf(player.getUniqueId()).orElse(null);
            if (current != captured || !current.leaderId().equals(leader)) {
                tell(player, ("&#FFB86C" + Messages.text("Clan atau leader berubah. Buka ulang menu.")));
                return;
            }
            if (failure != null || resolved == null || capturedGeneration != limits.generation()) { tell(player, ("&#FF5C8A" + Messages.text("Limit berubah atau gagal dibaca. Buka ulang menu."))); return; }
            try { action.accept(current, resolved); }
            catch (IllegalArgumentException | IllegalStateException error) { tell(player, "&#FF5C8A" + error.getMessage()); }
        }));
    }

    private void teleportHome(Player player, String name) {
        String serialized = clans.home(player.getUniqueId(), name).orElseThrow(() -> new IllegalStateException(Messages.text("Home sudah tidak tersedia")));
        String[] parts = serialized.split(",");
        if (parts.length != 6) throw new IllegalStateException(Messages.text("Lokasi home tidak valid"));
        var world = player.getServer().getWorld(parts[0]);
        if (world == null) throw new IllegalStateException(Messages.text("World home tidak tersedia"));
        double x = Double.parseDouble(parts[1]), y = Double.parseDouble(parts[2]), z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]), pitch = Float.parseFloat(parts[5]);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalStateException(Messages.text("Lokasi home tidak valid"));
        }
        if (!player.teleport(new Location(world, x, y, z, yaw, pitch))) tell(player, ("&#FFB86C" + Messages.text("Teleport dibatalkan.")));
        player.closeInventory();
    }

    private Clan requireClan(Player player) {
        return clans.clanOf(player.getUniqueId()).orElseThrow(() -> new IllegalStateException(Messages.text("Tidak memiliki clan")));
    }
    private void requirePermission(Player player, ClanMenuAction action) {
        String node = id.dragon.nagaclan.command.ClanPermissionPolicy.action(action);
        if (node != null && !player.hasPermission(node)) throw new IllegalStateException(Messages.text("Tidak memiliki permission untuk aksi ini"));
    }
    private void help(Player player, String command) {
        player.closeInventory();
        player.sendMessage(ClanColors.colorize(Messages.style(("&#A9B8FF" + Messages.text("Gunakan") + " ")) + "&f" + Messages.usage(command)));
    }
    private void createFromDialog(Player player) {
        UUID playerId = player.getUniqueId();
        UUID token = UUID.randomUUID();
        dialogSessions.put(playerId, token);
        try { NativeInputDialog.show(player, Messages.get("dialog.create.title"), "", value -> runSync(() -> {
            if (!player.isOnline() || !plugin.isEnabled() || !dialogSessions.remove(playerId, token)) return;
            if (value == null) { open(player, ClanMenuPage.MAIN, 0); return; }
            try {
                requirePermission(player, ClanMenuAction.CREATE_HELP);
                clans.create(playerId, value.strip());
                tell(player, "&#4CC9F0" + Messages.text("Clan dibuat."));
                open(player, ClanMenuPage.MAIN, 0);
            } catch (IllegalArgumentException | IllegalStateException failure) {
                tell(player, "&#FF5C8A" + failure.getMessage());
                open(player, ClanMenuPage.MAIN, 0);
            }
        })); }
        catch (RuntimeException | LinkageError failure) {
            dialogSessions.remove(playerId, token);
            help(player, "/clan create <nama>");
        }
    }
    private void runSync(Runnable task) {
        if (!plugin.isEnabled()) return;
        try { plugin.getServer().getScheduler().runTask(plugin, task); }
        catch (org.bukkit.plugin.IllegalPluginAccessException ignored) { /* Shutdown raced with an async limit result. */ }
    }
    public void forget(UUID player) { confirmations.remove(player); limits.invalidate(player); invalidateView(player); }
    public void onReload() {
        prompts.clear();
        plugin.reloadConfig();
        confirmations.clear();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean hadDialog = dialogSessions.containsKey(player.getUniqueId());
            invalidateView(player.getUniqueId());
            if (hadDialog || player.getOpenInventory().getTopInventory().getHolder() instanceof ClanMenuHolder) player.closeInventory();
        }
    }
    @Override public void close() {
        prompts.close(); confirmations.clear(); limits.invalidateAll(); dialogSessions.clear(); viewVersions.clear();
    }
    private static void tell(Player player, String text) { player.sendMessage(ClanColors.colorize(Messages.style(text))); }
}
