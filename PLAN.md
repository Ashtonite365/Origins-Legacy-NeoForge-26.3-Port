# Origins: Legacy → NeoForge 26.3.0.76 — scouting plan (personal use only, local only)

## 1. Source
- Project: Origins: Legacy by BluSpring (Modrinth `origins-legacy` / mc1uvRn8, CurseForge 1429195). Unofficial forward port of apace100's Origins 1.20.1.
- Repo: https://github.com/BluSpring/Origins-Legacy, branch `versions/26.3`, commit `84573621ab30e575e028adf68409e0c7c818e535` (2026-10-07, "bump version"). Cloned at ./src (default branch is versions/1.21.1; checked out 26.3).
  - Submodule apoli → https://github.com/BluSpring/apoli-legacy `versions/26.3` @ `1399f04d2900f094417d73462215ce6dd41419dc` (Apoli-Legacy 2.12.13)
  - Submodule calio → https://github.com/BluSpring/calio-legacy `versions/26.3` @ `0a9e8c7c208837550450586c1ade610b9abca548` (Calio-Legacy 1.11.4)
- Mod version 1.12.19, MC 26.3, Fabric Loader 0.19.5, Fabric API 0.160.5+26.3, Java 25, fabric-loom 1.17-SNAPSHOT.
- Release jar: ./Origins-Legacy-1.12.19+26.3.jar (Modrinth, 2026-10-07); embeds Apoli-Legacy-2.12.13+26.3, Calio-Legacy-1.11.4+26.3, mixinconstraints-1.0.8.
- Alternatives with similar names (not this one): official apace100 Origins (Fabric), Origins (Forge) ports by EdwinMindcraft / Origins-NeoForge (official-API rewrite, not datapack-1.20.1-compatible). Repo also has `rewrite/v2` branch (ignore).

## 2. Licence
- Origins-Legacy, apoli-legacy, calio-legacy: all MIT ("Copyright (c) 2020/2021 apace100"). Key clause: permission "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell", condition: "The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software." No warranty.
- Verdict: private personal port is allowed. Keep LICENSE files in the port.
- Assets: no separate asset licence found in repo; textures/lang fall under the repo MIT.
- Bundled libs: CCA MIT; PlayerAbilityLib LGPL-3.0; Additional Entity Attributes MIT; Cloth Config LGPL-3.0; MixinSquared MIT; mixinconstraints (MIT, Moulberry). LGPL is fine for private use (would only matter if redistributed).

## 3. Dependencies → NeoForge 26.3
| Dep (Fabric) | Used by | NeoForge 26.3 | Action |
|---|---|---|---|
| Fabric Loader / fabric.mod.json entrypoints | all | native | `@Mod` + `neoforge.mods.toml` |
| Fabric API networking v1 (21+10 refs) | all | native `RegisterPayloadHandlersEvent`, `PacketDistributor` | rewrite (CustomPacketPayload probably already used) |
| Fabric API events / lifecycle / entity events (~25 refs) | all | native NeoForge events | rewrite |
| resource-loader v1 / resource conditions (~21 refs) | calio/apoli/origins | `AddServerReloadListenersEvent`, `ICondition` | rewrite (calio data loaders = core) |
| command v2, keymapping, client rendering/HUD, creativetab, loot, registry events | various | `RegisterCommandsEvent`, `RegisterKeyMappingsEvent`, `RegisterGuiLayersEvent`, `BuildCreativeModeTabContentsEvent`, `LootTableLoadEvent`, `RegisterEvent`/DeferredRegister | rewrite |
| Cardinal Components base+entity 8.0.1 | apoli/origins components | **no NeoForge build** | native Data Attachments (`AttachmentType` + `syncWith`/custom sync) |
| PlayerAbilityLib 1.16.0 | apoli (5 files) | **no NeoForge build** (BLOCKER-lite) | stub/reimplement small ability-source tracker over `Abilities` |
| Additional Entity Attributes 2.2.4 | apoli (4 files) | **no 26.3 NeoForge build found** (project lists neoforge loader for older MC) | register the few attributes ourselves or port AEA |
| Cloth Config 26.3.158 | 12 files | yes: 26.3.159+neoforge (Modrinth) | swap artifact |
| ModMenu 21.0.0-beta.1 | integration | n/a | drop; use `IConfigScreenFactory` |
| MixinExtras (22 files) | apoli | native (NeoForge ships MixinExtras) | none |
| MixinSquared 0.3.7-beta.3 | apoli | has neoforge artifact (`mixinsquared-neoforge`) — verify 26.3 | swap or remove |
| mixinconstraints 1.0.8 | origins | multi-loader | keep or remove |
| mcdev annotations | apoli | compile-only | keep |

Blockers: CCA (→ attachments, biggest refactor), PAL (stub), AEA (no 26.3 NF build). Nothing hard-blocking.

## 4. Mixins & access wideners
- 3 mixin configs: origins.mixins.json (10 common / 3 client), apoli.mixins.json (52 / 26), calio.mixins.json (6 / 3). **Total 100 entries: 68 common, 32 client.** ~89 mixin source files (12 origins, 77 apoli incl. legacy/powers subpkgs, 8 calio).
- Access wideners: apoli.accesswidener 3 live entries (+1 commented): mutable ItemStack.item, extendable ItemStack, accessible Level.DEFAULT_EXPLOSION_BLOCK_PARTICLES. calio.accesswidener 0. → 3 AT lines (note: `mutable` = `public-f`).
- Riskiest targets: ItemStack (5 mixins + mutable item field), LivingEntity/Entity/Player (core of power hooks), PlayerList & ServerPlayerGameMode & MultiPlayerGameMode (respawn/origin sync, block breaking), RecipeManager/CraftingMenu/TransientCraftingContainer (recipe powers), ArgumentTypeInfos (NeoForge has own registration – drop mixin), Hud/ScreenEffectRenderer/LevelRenderer/FogRenderer/WingsLayer/Window (client render – overlap NeoForge render hooks), ServerGamePacketListenerImpl, ShufflingList. 1 file uses @Redirect/@Overwrite. Several Fabric-API-internal mixins likely become events.

## 5. Size (Java; 0 Kotlin) — total 486
- origins 81: mixin 12, networking 11, util 10, registry 8, origin 8, badge 8, screen 4, power 3, integration 3, component 3, command 3, data 2, content 2, root 2, entity 1, screen/tooltip 1
- apoli 363: power 120, mixin 77, util 35, factory/action/* 43, factory/condition/* 26, access 15, networking 12, integration 9, command 4, registry 3, global 3, data 3, component 3, root 3, legacy/* 4, screen 2
- calio 42: util 10, data 7, root 7, mixin 8, registry 4, resource 3, network 3

## 6. Split (balanced ~80 files each)
- **H1 (first, blocking)**: build (NeoForge 26.3.0.76 MDK, single module or 3 source sets), neoforge.mods.toml, AT, platform layer (`Platform` shim for networking/events/registries/attachments), all of **calio** (42) + apoli root/registry/component/networking/global/data + origins root/registry/component/networking. CCA→attachments, PAL stub, AEA attributes.
- **H2**: apoli/power A–M (~60 of 120).
- **H3**: apoli/power N–Z (~60) + apoli/legacy, power/legacy.
- **H4**: apoli/power/factory/** (action + condition, ~69) + apoli/util, util/modifier (~35) — mostly mechanical import fixes.
- **H5**: all mixins (apoli 77 + origins 12 + calio 8), migrating to events where Fabric-API-only; access/ duck interfaces (15).
- **H6**: client & integration: apoli/origins screen, badge, origins origin/power/util/command/data/content/entity, integration (cloth config screen via IConfigScreenFactory, drop ModMenu), keybinds, HUD layers.

## Approach notes
- Entrypoints: ModInitializer/ClientModInitializer → `@Mod` ctor + `@Mod(dist=CLIENT)`; the 3 jars become one mod or 3 mods in one jar (simpler: one mod id `origins` with apoli/calio as embedded packages, keep namespaces `apoli`,`calio` for data).
- Events: Fabric `Event` callbacks → `NeoForge.EVENT_BUS` / mod bus listeners.
- Networking: `PayloadTypeRegistry`/`ServerPlayNetworking` → `RegisterPayloadHandlersEvent` + `PacketDistributor.sendToPlayer`.
- Attachments: CCA `ComponentKey`/`AutoSyncedComponent` → `AttachmentType` with codec + `copyOnDeath` + manual sync payload.
- AW → `META-INF/accesstransformer.cfg` (3 lines); mixin configs referenced from mods.toml `[[mixins]]`.
- Registries: Fabric `Registry.register` → `DeferredRegister`; custom registries via `NewRegistryEvent`.
- Reusable tools in /workspace/mod-port: ars-compile.sh (compile loop), bloodmagic/tools/{sigmod.py, codemod.py, fiximports.py, addimport.py, gui_codemod.py, linefix.py, owner_errors.py}, plus top-level codemod.py/errs.py/fe.py; mdk/ for template. No runClient.

## Port repo (flattened)
- /workspace/mod-port/origins-legacy/port, one tree: src/main/java has io.github.apace100.{origins,apoli,calio}.
- Upstream commits: Origins-Legacy `84573621ab30e575e028adf68409e0c7c818e535`, apoli-legacy `1399f04d2900f094417d73462215ce6dd41419dc`, calio-legacy `0a9e8c7c208837550450586c1ade610b9abca548` (all `versions/26.3`).
- Original fabric.mod.json files and access wideners kept in port-reference/. Tag collisions (bypasses_armor, bypasses_shield, no_knockback) merged.
