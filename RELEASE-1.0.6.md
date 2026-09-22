# NagaClan 1.0.6

## Upgrade

Stop the server, back up the existing NagaClan data directory, and replace only the old NagaClan JAR. Keep `clans.yml` and `config.yml`. Do not run two NagaClan JARs together. Requires NagaCore 1.1.0+ and Java 21.

New configuration keys are optional; existing configuration files continue working:

```yaml
language: id # en, id, es, ru, zh
text:
  small-caps: false
dialogs:
  enabled: true
  proxy-translation: false
```

The five editable UTF-8 language files are created under `plugins/NagaClan/languages/`. Server-wide language selection applies to player messages, default menu labels and titles, dialog inputs and command guidance. Custom administrator-written menu titles remain unchanged. Material identifiers, commands, player names, clan names, prefixes and chat contents are not translated. Missing custom-file entries use the bundled language, then English.

Use `/nagacore reload NagaClan` after changing configuration or language files. Invalid configuration is rejected. Reload invalidates old input sessions and closes clan inventories. A stale dialog callback cannot save a change.

## Revision

- Clan chat: private recipient list, public event cancellation and cleared public audience; literal `[Teams Chat]` label. The default font is normal text.
- `/clan settings` opens settings directly. Access settings exposes 15 role thresholds.
- Leadership transfer, disband and access-policy editing remain leader-only. Server permissions and target-role hierarchy are still enforced.
- Icon menu includes **Custom material**. Enter an item name such as `DIAMOND` or `GOLD_INGOT`; air, non-items and unknown materials are rejected. The configured icon list is now a preset palette, not a restriction on custom item icons.
- Icon and prefix input use single-use, 60-second dialogs where supported; otherwise private chat. Type `cancel` in chat to abort. Leader changes, clan changes, revoked access, expired input and reload invalidate pending edits.
- Dialogs require a Java client protocol of at least 1.21.6 and an available Paper Dialog API (Paper 1.21.7+). ViaVersion on the backend is queried when present. Set `proxy-translation: true` when translation exists only on the proxy. Unsupported or unknown clients fall back to chat.
- `/clan delhome <name>` deletes a saved home. It has its own permission and role threshold, tab-completes existing names and rolls back if saving fails.
- Role policies persist in `clans.yml`; legacy member-bank booleans retain their old meaning.

## Verification and remaining manual checks

95 automated regression tests cover language-key parity, actual chat-event cancellation/public-audience suppression, clan-only recipients, role delegation and hierarchy, bank access checks, policy rollback, home-deletion rollback, storage round trips, legacy migration and dialog protocol selection. The workspace build runs the same suite against compiled source and the packaged JAR.

This build has **not** been run inside a live Minecraft server during this revision. Before production, test:

1. Two same-clan players and one outsider: enable clan chat, send a message and verify the outsider receives nothing; disable it and verify public chat resumes.
2. A modern Java client and an older ViaBackwards client: custom icon and prefix input, Done, Cancel, Escape and expiration.
3. Each language, including Russian and Chinese item text, plus live language reload.
4. Delegated access, permissions revoked while a menu is open, leadership transfer during input and server restart.
5. Vault deposit/withdraw permissions with your actual economy provider.

Read `SPIGOT-DESCRIPTION.md` for the publication draft and marketplace restrictions. The NagaCore licensing requirement has not been removed.
