# NagaClan — Modern clans for Paper

Bring your players together with private team chat, a clan dashboard, shared homes, a clan bank and leader-controlled role permissions.

## Features

- Private clan chat: toggle with `/clan chat`, or send a single message with `/clan chat <message>`. Messages are delivered only to current clan members and carry the **[Teams Chat]** label.
- Inventory menus for members, homes, banking, alliances, settings and clan rankings.
- Leader, Admin, Builder and Member roles, with configurable action access from `/clan settings`.
- Separate role thresholds for chat, home teleport, setting/deleting homes, inviting, kicking, alliances, role assignment, deposits, withdrawals, friendly fire, leaderboard visibility, prefixes and icons.
- Shared clan homes with `/clan sethome <name>`, `/clan home <name>` and `/clan delhome <name>`.
- Shared clan bank through Vault and a compatible economy provider.
- Leader-based home and bank limits. A member's donor rank does not increase the clan's limits.
- Editable clan prefixes, custom item-material icons and optional leaderboard visibility.
- Native text-input dialogs for supported Java clients and Paper versions, with private chat input as the fallback.
- English, Indonesian, Spanish, Russian and Simplified Chinese language files.
- Permission-aware command suggestions and confirmation before destructive clan actions.
- Optional PlaceholderAPI integration.
- Reload through `/nagacore reload NagaClan`; existing `/nc reload` and `/nagaclan reload` aliases remain available.

## Requirements and compatibility

**Paper server, Java 21, and NagaCore 1.1.0 or newer are required.**

This is a Paper plugin, **not a plain Spigot-server plugin**. Version 1.0.6 is compiled against Paper 1.21.8. Do not advertise untested server versions as tested.

Minecraft Java dialogs require a 1.21.6+ client. The Paper Dialog API is available from Paper 1.21.7; older servers or unsupported/unknown clients use private chat input instead. A modern client alone does not add dialog support to an older server.

ViaVersion installed on the Paper backend is detected to obtain the original client version. If protocol translation runs only on a proxy, set `dialogs.proxy-translation: true` to use the safe chat fallback. Set `dialogs.enabled: false` to disable dialogs for everyone.

Optional dependencies:

- **Vault + a Vault-compatible economy provider:** clan banking.
- **LuckPerms:** offline leader permission lookup for group-based limits.
- **PlaceholderAPI:** clan placeholders.
- **ViaVersion:** original client-version detection when installed on the backend.

NagaCore remote license verification must succeed for this edition to operate. This dependency and requirement must be disclosed before distribution.

## Installation

1. Stop the server and back up `plugins/NagaClan/`.
2. Install the required, valid NagaCore version.
3. Remove the old NagaClan JAR from the plugins directory, retaining its data folder.
4. Install `NagaClan-1.0.6.jar`, plus any optional integrations you need.
5. Start the server. Choose `language: en`, `id`, `es`, `ru` or `zh` in `config.yml`.
6. Adjust translations in `plugins/NagaClan/languages/`, then reload NagaClan.

Existing clan data is retained. Old bank-access booleans migrate to the equivalent role thresholds.

## Commands

Player commands:

- `/clan` or `/clan menu` — open the dashboard.
- `/clan create <name>` — create a clan.
- `/clan invite <player>`, `accept`, `deny`, `kick <player>`, `leave`.
- `/clan chat [message]` — toggle private chat or send a single private message.
- `/clan settings` — clan settings and access-policy menus.
- `/clan sethome <name>`, `home <name>`, `delhome <name>`.
- `/clan setbuilder <player>`, `setadmin <player>`, `setleader <player>`.
- `/clan alliance <clan>` — record an alliance in your clan's list.
- `/clan top`, `/clan level`.
- `/clan disband` — repeat within 30 seconds to confirm.

Administration:

- `/nagacore reload NagaClan`
- `/nc reload|save|version`
- `/nagaclan reload|save|version`

Players without a clan receive only create, accept and deny as first-argument suggestions.

## Permissions and access

Ordinary command nodes use `nagaclan.command.<subcommand>`, except clan chat, which uses `nagaclan.chat`.

Additional nodes:

- `nagaclan.settings` — edit clan settings, subject to clan-role access.
- `nagaclan.bank.deposit`, `nagaclan.bank.withdraw`.
- `nagaclan.bank.settings` — legacy member-access switches, leader only.
- `nagaclan.admin.reload`, `nagaclan.admin.save`, `nagaclan.admin.version`.
- `nagacore.admin.reload` — central reload command.

Leader limit tiers are defined in `group-limits.tiers`. The default prefixes are `nagaclans.sethome.multiple.<tier>` and `nagaclans.maxbank.multiple.<tier>`.

Clan access settings do not grant Bukkit/LuckPerms permissions. Higher-ranking roles inherit the configured action access. A player cannot kick or promote someone at or above their own rank. Only the leader can change access policies, transfer leadership or disband the clan.

## Placeholders

`%nagaclan_name%`, `%nagaclan_prefix%`, `%nagaclan_level%`, `%nagaclan_role%`, `%nagaclan_bank%`.

## Support and credits

Developed for **NagaDEV**. TikTok: **@nagasak**.

For a bug report, include the NagaClan/NagaCore versions, Paper version, client version, relevant console log and reproduction steps. Remove passwords and private license information before sharing logs.

---

## Publisher checklist — remove this section from the public description

- Add your actual support URL, pricing/distribution terms and fresh in-game screenshots. No invented links are included.
- Test on a live Paper server before marking any Minecraft version as tested. Automated tests are not live compatibility certification.
- This description does **not** imply marketplace approval or plain Spigot runtime compatibility.
- **Do not submit this licensed build as a Spigot Premium resource unchanged.** The published Premium Resource Guidelines prohibit licensing systems requiring activation/specific-server access and require support for the latest stable Spigot. This build remains Paper-only and NagaCore-license-dependent, as requested; no licensing bypass has been introduced.
- Decide on a separately authorized compliant distribution edition or a suitable marketplace before publishing.

References checked on 2026-09-20:

- [Paper Dialog API](https://docs.papermc.io/paper/dev/dialogs/)
- [Spigot Premium Resource Guidelines](https://www.spigotmc.org/threads/premium-resource-guidelines.31667/)
