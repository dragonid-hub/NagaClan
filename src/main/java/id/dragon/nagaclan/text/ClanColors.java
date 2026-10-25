package id.dragon.nagaclan.text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Standalone legacy/hex colour conversion for messages, menus and placeholders. */
public final class ClanColors {
    private static final Pattern HEX = Pattern.compile("(?i)&#([0-9a-f]{6})|<#([0-9a-f]{6})>|(?<![\\w&<])#([0-9a-f]{6})(?![0-9a-f])");
    private static final Pattern SPACED_HEX = Pattern.compile("(?i)&x(?:&[0-9a-f]){6}");
    private static final Pattern LEGACY = Pattern.compile("(?i)&([0-9a-fk-or])");
    private ClanColors() { }

    public static String colorize(String input) {
        if (input == null) return null;
        Matcher hex = HEX.matcher(input);
        StringBuilder expanded = new StringBuilder();
        while (hex.find()) {
            String rgb = hex.group(1) != null ? hex.group(1) : hex.group(2) != null ? hex.group(2) : hex.group(3);
            StringBuilder nativeHex = new StringBuilder("§x");
            for (char digit : rgb.toCharArray()) nativeHex.append('§').append(Character.toUpperCase(digit));
            hex.appendReplacement(expanded, Matcher.quoteReplacement(nativeHex.toString()));
        }
        hex.appendTail(expanded);
        Matcher spacedHex = SPACED_HEX.matcher(expanded);
        StringBuilder normalized = new StringBuilder();
        while (spacedHex.find()) spacedHex.appendReplacement(normalized,
                Matcher.quoteReplacement(spacedHex.group().replace('&', '§')));
        spacedHex.appendTail(normalized);
        Matcher legacy = LEGACY.matcher(normalized);
        StringBuilder output = new StringBuilder();
        while (legacy.find()) legacy.appendReplacement(output, Matcher.quoteReplacement("§" + legacy.group(1).toLowerCase(java.util.Locale.ROOT)));
        legacy.appendTail(output);
        return output.toString();
    }
}
