# NagaClan 1.0.8 — standalone edition

NagaClan no longer requires NagaCore to start, run commands, render menus, update
placeholders, colorize messages or reload configuration. Install it on Paper 1.21.8
with Java 21. NagaCore may be installed for unrelated plugins, but is ignored by
this build.

Upgrade: stop the server, back up `plugins/NagaClan/`, remove older NagaClan JARs,
place `NagaClan-1.0.8.jar` in `plugins/`, and start the server. Keep your existing
`clans.yml`, `config.yml` and language files. Existing settings are not deleted.
Use `/nc reload` or `/nagaclan reload` for configuration changes. The former
`/nagacore reload NagaClan` integration is no longer registered.

Vault and an economy provider are still needed for clan bank transactions;
PlaceholderAPI, LuckPerms and ViaVersion remain optional integrations.

Source and packaged regression suites passed 97 tests each. NagaRTP 1.2.3 was
also rebuilt unchanged and passed 5,059 checks. A live Paper-server smoke test
has not been performed in this workspace.
