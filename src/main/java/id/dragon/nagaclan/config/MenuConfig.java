package id.dragon.nagaclan.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Validated presentation values used by NagaClan inventory renderers. */
public final class MenuConfig {
    private static final List<Long> DEFAULT_BANK_AMOUNTS = List.of(1_000L, 10_000L, 100_000L);
    private static final List<Material> DEFAULT_ALLOWED_ICONS = List.of(
            Material.AMETHYST_SHARD, Material.DIAMOND, Material.NETHER_STAR, Material.DRAGON_HEAD, Material.SHIELD);
    private static final Border DEFAULT_BORDER = new Border(
            Material.LIGHT_BLUE_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE,
            List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53));
    private static final Map<String, Page> DEFAULT_PAGES = Map.ofEntries(
            Map.entry("main", new Page("&#6D5DFB&lNagaClan &#4CC9F0Dashboard", 54)),
            Map.entry("members", new Page("&#6D5DFB&lNagaClan &#4CC9F0Members", 54)),
            Map.entry("homes", new Page("&#6D5DFB&lNagaClan &#4CC9F0Homes", 54)),
            Map.entry("bank", new Page("&#6D5DFB&lNagaClan &#A78BFABank", 54)),
            Map.entry("settings", new Page("&#6D5DFB&lNagaClan &#A78BFASettings", 54)),
            Map.entry("alliances", new Page("&#6D5DFB&lNagaClan &#4CC9F0Alliances", 54)),
            Map.entry("top", new Page("&#6D5DFB&lNagaClan &#A78BFATop Clans", 54)));
    private static final Map<String, Button> DEFAULT_BUTTONS = Map.ofEntries(
            Map.entry("main.members", new Button(Material.PLAYER_HEAD, 20)),
            Map.entry("main.homes", new Button(Material.ENDER_PEARL, 22)),
            Map.entry("main.bank", new Button(Material.GOLD_INGOT, 24)),
            Map.entry("main.alliances", new Button(Material.HEART_OF_THE_SEA, 29)),
            Map.entry("main.settings", new Button(Material.COMPARATOR, 31)),
            Map.entry("main.top", new Button(Material.NETHER_STAR, 33)),
            Map.entry("main.leave", new Button(Material.BARRIER, 49)),
            Map.entry("navigation.previous", new Button(Material.ARROW, 45)),
            Map.entry("navigation.page", new Button(Material.PAPER, 49)),
            Map.entry("navigation.next", new Button(Material.ARROW, 53)),
            Map.entry("navigation.close", new Button(Material.BARRIER, 53)));
    private static final Map<String, String> DEFAULT_MESSAGES = Map.of(
            "prefix", "&#FF9A00&lN&#FF8600&la&#FF7100&lg&#FF5D00&la&#0076FF&lC&#007FFF&ll&#0088FF&la&#0091FF&ln &8» &7",
            "no-clan", "&#A9B8FFBuat clan dengan &f/clan create <nama>&#A9B8FF.",
            "shadow", "&8");

    private final Map<String, Page> pages;
    private final Border border;
    private final Map<String, Button> buttons;
    private final List<Long> bankAmounts;
    private final List<Material> allowedIcons;
    private final Map<String, String> messages;

    private MenuConfig(Map<String, Page> pages, Border border, Map<String, Button> buttons,
                       List<Long> bankAmounts, List<Material> allowedIcons, Map<String, String> messages) {
        this.pages = Map.copyOf(pages);
        this.border = border;
        this.buttons = Map.copyOf(buttons);
        this.bankAmounts = List.copyOf(bankAmounts);
        this.allowedIcons = List.copyOf(allowedIcons);
        this.messages = Map.copyOf(messages);
    }

    public static MenuConfig from(ConfigurationSection root, Consumer<String> warningConsumer) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(warningConsumer, "warningConsumer");
        Map<String, Page> pages = parsePages(root, warningConsumer);
        Border border = parseBorder(root, pages, warningConsumer);
        Map<String, Button> buttons = parseButtons(root, pages, warningConsumer);
        List<Long> bankAmounts = parseAmounts(root, warningConsumer);
        List<Material> allowedIcons = parseIcons(root, warningConsumer);
        Map<String, String> messages = parseMessages(root, warningConsumer);
        return new MenuConfig(pages, border, buttons, bankAmounts, allowedIcons, messages);
    }

    public Page page(String name) {
        Page page = pages.get(normalizeKey(name));
        if (page == null) {
            throw new IllegalArgumentException("Unknown menu page: " + name);
        }
        return page;
    }

    public Map<String, Page> pages() {
        return pages;
    }

    public Border border() {
        return border;
    }

    public Button button(String name) {
        Button button = buttons.get(normalizeKey(name));
        if (button == null) {
            throw new IllegalArgumentException("Unknown menu button: " + name);
        }
        return button;
    }

    public Map<String, Button> buttons() {
        return buttons;
    }

    public List<Long> bankAmounts() {
        return bankAmounts;
    }

    public List<Material> allowedIcons() {
        return allowedIcons;
    }

    public String message(String name) {
        String message = messages.get(normalizeKey(name));
        if (message == null) {
            throw new IllegalArgumentException("Unknown fixed message: " + name);
        }
        return message;
    }

    public Map<String, String> messages() {
        return messages;
    }

    private static Map<String, Page> parsePages(ConfigurationSection root, Consumer<String> warning) {
        Map<String, Page> pages = new LinkedHashMap<>();
        DEFAULT_PAGES.forEach((name, fallback) -> {
            String base = "gui." + name;
            String titlePath = base + ".title";
            String title = root.getString(titlePath);
            if (title == null || title.isBlank()) {
                title = fallback.title();
                warn(warning, titlePath);
            }
            String sizePath = base + ".size";
            Object configuredSize = root.get(sizePath);
            Long parsedSize = wholeNumber(configuredSize);
            int size = parsedSize != null && fitsInt(parsedSize) ? parsedSize.intValue() : fallback.size();
            if (configuredSize != null && (parsedSize == null || !fitsInt(parsedSize) || !validSize(size)
                    || (name.equals("main") && size != fallback.size()))) {
                size = fallback.size();
                warn(warning, sizePath);
            }
            pages.put(name, new Page(title, size));
        });
        return pages;
    }

    private static Border parseBorder(ConfigurationSection root, Map<String, Page> pages, Consumer<String> warning) {
        Material primary = material(root.getString("gui.theme.border.primary-material"), DEFAULT_BORDER.primaryMaterial(),
                "gui.theme.border.primary-material", warning);
        Material secondary = material(root.getString("gui.theme.border.secondary-material"), DEFAULT_BORDER.secondaryMaterial(),
                "gui.theme.border.secondary-material", warning);
        int pageSize = minimumPageSize(pages);
        List<Integer> slots = new ArrayList<>();
        List<?> configured = root.getList("gui.theme.border.slots");
        if (configured == null || configured.isEmpty()) {
            warn(warning, "gui.theme.border.slots");
            slots.addAll(fallbackBorderSlots(pageSize));
        } else {
            for (int index = 0; index < configured.size(); index++) {
                Object value = configured.get(index);
                Long slot = wholeNumber(value);
                if (slot != null && fitsInt(slot) && validSlot(slot.intValue(), pageSize)) {
                    slots.add(slot.intValue());
                } else {
                    warn(warning, "gui.theme.border.slots[" + index + "]");
                }
            }
            if (slots.isEmpty()) {
                warn(warning, "gui.theme.border.slots");
                slots.addAll(fallbackBorderSlots(pageSize));
            }
        }
        return new Border(primary, secondary, slots);
    }

    private static Map<String, Button> parseButtons(ConfigurationSection root, Map<String, Page> pages, Consumer<String> warning) {
        Map<String, Button> buttons = new LinkedHashMap<>();
        HashSet<Integer> claimedMainSlots = new HashSet<>(java.util.Set.of(13)); // Dashboard summary.
        int mainSize = pages.get("main").size();
        for (String name : List.of("main.members", "main.homes", "main.bank", "main.alliances", "main.settings", "main.top", "main.leave")) {
            Button fallback = DEFAULT_BUTTONS.get(name);
            Button value = parseButton(root, name, fallback, mainSize, fallback.slot(), warning);
            if (claimedMainSlots.contains(value.slot())) {
                warn(warning, buttonPath(name) + ".slot");
                value = new Button(value.material(), availableNavigationSlot(fallback.slot(), mainSize, claimedMainSlots));
            }
            claimedMainSlots.add(value.slot());
            buttons.put(name, value);
        }

        int navigationSize = minimumPageSize(pages);
        HashSet<Integer> claimedNavigationSlots = new HashSet<>();
        for (String name : List.of("navigation.previous", "navigation.page", "navigation.next")) {
            Button fallback = DEFAULT_BUTTONS.get(name);
            buttons.put(name, parseNavigationButton(root, name, fallback, navigationSize, claimedNavigationSlots, warning));
        }
        Button closeFallback = DEFAULT_BUTTONS.get("navigation.close");
        buttons.put("navigation.close", parseButton(root, "navigation.close", closeFallback, navigationSize,
                navigationDefaultSlot("navigation.next", navigationSize), warning));
        return buttons;
    }

    private static Button parseNavigationButton(ConfigurationSection root, String name, Button fallback, int pageSize,
                                                HashSet<Integer> claimedSlots, Consumer<String> warning) {
        int defaultSlot = navigationDefaultSlot(name, pageSize);
        Button button = parseButton(root, name, fallback, pageSize, defaultSlot, warning);
        int slot = button.slot();
        if (claimedSlots.contains(slot)) {
            warn(warning, buttonPath(name) + ".slot");
            slot = availableNavigationSlot(defaultSlot, pageSize, claimedSlots);
            button = new Button(button.material(), slot);
        }
        claimedSlots.add(slot);
        return button;
    }

    private static Button parseButton(ConfigurationSection root, String name, Button fallback, int pageSize,
                                      int fallbackSlot, Consumer<String> warning) {
        String base = buttonPath(name);
        Material material = material(root.getString(base + ".material"), fallback.material(), base + ".material", warning);
        Object configuredSlot = root.get(base + ".slot");
        Long parsedSlot = wholeNumber(configuredSlot);
        int slot = parsedSlot != null && fitsInt(parsedSlot) ? parsedSlot.intValue() : fallbackSlot;
        if (configuredSlot != null && (parsedSlot == null || !fitsInt(parsedSlot))) {
            warn(warning, base + ".slot");
        }
        if (!validSlot(slot, pageSize)) {
            slot = fallbackSlot;
            warn(warning, base + ".slot");
        }
        return new Button(material, slot);
    }

    private static List<Long> parseAmounts(ConfigurationSection root, Consumer<String> warning) {
        List<?> configured = root.getList("bank.amounts");
        if (configured == null || configured.isEmpty()) {
            warn(warning, "bank.amounts");
            return DEFAULT_BANK_AMOUNTS;
        }
        List<Long> amounts = new ArrayList<>();
        for (int index = 0; index < configured.size(); index++) {
            Object value = configured.get(index);
            Long amount = wholeNumber(value);
            if (amount != null && amount > 0L) {
                amounts.add(amount);
            } else {
                warn(warning, "bank.amounts[" + index + "]");
            }
        }
        if (amounts.isEmpty()) {
            warn(warning, "bank.amounts");
            return DEFAULT_BANK_AMOUNTS;
        }
        return amounts;
    }

    private static List<Material> parseIcons(ConfigurationSection root, Consumer<String> warning) {
        List<?> configured = root.getList("settings.allowed-icons");
        if (configured == null || configured.isEmpty()) {
            warn(warning, "settings.allowed-icons");
            return DEFAULT_ALLOWED_ICONS;
        }
        List<Material> icons = new ArrayList<>();
        for (int index = 0; index < configured.size(); index++) {
            Object value = configured.get(index);
            Material icon = value instanceof String text ? Material.matchMaterial(text) : null;
            if (icon == null || isAir(icon)) {
                warn(warning, "settings.allowed-icons[" + index + "]");
            } else if (!icons.contains(icon)) {
                icons.add(icon);
            }
        }
        if (icons.isEmpty()) {
            warn(warning, "settings.allowed-icons");
            return DEFAULT_ALLOWED_ICONS;
        }
        return icons;
    }

    private static Map<String, String> parseMessages(ConfigurationSection root, Consumer<String> warning) {
        Map<String, String> messages = new LinkedHashMap<>();
        DEFAULT_MESSAGES.forEach((name, fallback) -> {
            String path = "messages." + name;
            String value = root.getString(path);
            if (value == null) {
                value = fallback;
                warn(warning, path);
            }
            if (name.equals("prefix") && value.equals("&#6D5DFB&lNaga&#4CC9F0&lClan &8» ")) value = fallback;
            messages.put(name, value);
        });
        return messages;
    }

    private static Material material(String configured, Material fallback, String path, Consumer<String> warning) {
        Material material = configured == null ? null : Material.matchMaterial(configured);
        if (material == null || isAir(material)) {
            warn(warning, path);
            return fallback;
        }
        return material;
    }

    private static boolean validSize(int size) {
        return size >= 9 && size <= 54 && size % 9 == 0;
    }

    private static boolean validSlot(int slot, int size) {
        return slot >= 0 && slot < size;
    }

    private static Long wholeNumber(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        try {
            return new BigDecimal(number.toString()).longValueExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            return null;
        }
    }

    private static boolean fitsInt(long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    private static int fallbackSlot(int configuredDefault, int pageSize) {
        return Math.min(configuredDefault, pageSize - 1);
    }

    private static List<Integer> fallbackBorderSlots(int pageSize) {
        return DEFAULT_BORDER.slots().stream().filter(slot -> validSlot(slot, pageSize)).toList();
    }

    private static String buttonPath(String name) {
        return name.startsWith("main.") ? "gui.main.buttons." + name.substring("main.".length())
                : "gui.navigation." + name.substring("navigation.".length());
    }

    private static int navigationDefaultSlot(String name, int pageSize) {
        int rowStart = pageSize - 9;
        return switch (name) {
            case "navigation.previous" -> rowStart;
            case "navigation.page" -> rowStart + 4;
            case "navigation.next" -> rowStart + 8;
            default -> throw new IllegalArgumentException("Unknown navigation button: " + name);
        };
    }

    private static int availableNavigationSlot(int preferredSlot, int pageSize, HashSet<Integer> claimedSlots) {
        if (!claimedSlots.contains(preferredSlot)) {
            return preferredSlot;
        }
        for (int slot = pageSize - 9; slot < pageSize; slot++) {
            if (!claimedSlots.contains(slot)) {
                return slot;
            }
        }
        throw new IllegalStateException("No available navigation slots");
    }

    private static int minimumPageSize(Map<String, Page> pages) {
        return pages.values().stream().mapToInt(Page::size).min().orElse(54);
    }

    private static void warn(Consumer<String> warning, String path) {
        warning.accept("Invalid optional configuration at " + path + "; using a safe default.");
    }

    private static String normalizeKey(String name) {
        return Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT);
    }

    public record Page(String title, int size) {
        public Page {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Page title must not be blank");
            }
            if (!validSize(size)) {
                throw new IllegalArgumentException("Page size must be a multiple of nine from 9 through 54");
            }
        }
    }

    public record Border(Material primaryMaterial, Material secondaryMaterial, List<Integer> slots) {
        public Border {
            primaryMaterial = validMaterial(primaryMaterial, "primaryMaterial");
            secondaryMaterial = validMaterial(secondaryMaterial, "secondaryMaterial");
            slots = List.copyOf(slots);
            if (slots.stream().anyMatch(slot -> !validSlot(slot, 54))) {
                throw new IllegalArgumentException("Border slots must fit a six-row inventory");
            }
        }
    }

    public record Button(Material material, int slot) {
        public Button {
            material = validMaterial(material, "material");
            if (!validSlot(slot, 54)) {
                throw new IllegalArgumentException("Button slots must fit a six-row inventory");
            }
        }
    }

    private static Material validMaterial(Material material, String name) {
        if (material == null || isAir(material)) {
            throw new IllegalArgumentException(name + " must be a non-air material");
        }
        return material;
    }

    private static boolean isAir(Material material) {
        return material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }
}
