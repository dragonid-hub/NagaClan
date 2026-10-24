package id.dragon.nagaclan.text;

import java.util.Map;

/** Converts display copy while preserving Minecraft colour tokens. */
public final class SmallCaps {
    private static final Map<Character, Character> LETTERS = Map.ofEntries(
            Map.entry('a', 'ᴀ'), Map.entry('b', 'ʙ'), Map.entry('c', 'ᴄ'), Map.entry('d', 'ᴅ'),
            Map.entry('e', 'ᴇ'), Map.entry('f', 'ꜰ'), Map.entry('g', 'ɢ'), Map.entry('h', 'ʜ'),
            Map.entry('i', 'ɪ'), Map.entry('j', 'ᴊ'), Map.entry('k', 'ᴋ'), Map.entry('l', 'ʟ'),
            Map.entry('m', 'ᴍ'), Map.entry('n', 'ɴ'), Map.entry('o', 'ᴏ'), Map.entry('p', 'ᴘ'),
            Map.entry('q', 'ꞯ'), Map.entry('r', 'ʀ'), Map.entry('s', 'ꜱ'), Map.entry('t', 'ᴛ'),
            Map.entry('u', 'ᴜ'), Map.entry('v', 'ᴠ'), Map.entry('w', 'ᴡ'), Map.entry('x', 'x'),
            Map.entry('y', 'ʏ'), Map.entry('z', 'ᴢ'));

    private SmallCaps() {
    }

    public static String format(String text) {
        if (text == null) return null;
        StringBuilder output = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            int tokenLength = colourTokenLength(text, index);
            if (tokenLength > 0) {
                output.append(text, index, index + tokenLength);
                index += tokenLength - 1;
                continue;
            }
            char current = text.charAt(index);
            output.append(LETTERS.getOrDefault(Character.toLowerCase(current), current));
        }
        return output.toString();
    }

    private static int colourTokenLength(String text, int index) {
        if (text.startsWith("&#", index) && index + 8 <= text.length()) return 8;
        if (text.startsWith("<#", index) && index + 9 <= text.length()) return 9;
        if (text.charAt(index) == '#' && index + 7 <= text.length()) return 7;
        if ((text.charAt(index) == '&' || text.charAt(index) == '§') && index + 2 <= text.length()) return 2;
        return 0;
    }
}
