# Origins: Legacy → NeoForge 26.3.0.76 — work split (personal port, local only)

Repo: /workspace/mod-port/origins-legacy/port (branch ol-integration). Owners: OWNERS.tsv (exact path wins, else longest `/**` glob).

| Owner | Scope |
|---|---|
| H1 (FIRST) | build files, META-INF (mods.toml, AT), platform layer, root classes, registries, components, networking, global/data (apoli+origins), all of Calio except its mixins; CCA→attachments, PAL stub, AEA attributes |
| H2 | apoli/power/*.java A–M |
| H3 | apoli/power/*.java N–Z, apoli/power/legacy, apoli/legacy |
| H4 | apoli/power/factory/**, apoli/util/** |
| H5 | all mixin packages (apoli, origins, calio), apoli/access, *.mixins.json |
| H6 | client & integration: screens, badge, integration, commands, origins/origin|power|util|data|content|entity, assets/data |

## Rules
- Edit only files you own (OWNERS.tsv). Need a change elsewhere? ask the owner / H1.
- Commit your claims and work in your own worktree /workspace/mod-port/ol-wt-hN on branch ol-hN.
- Never push or publish. Never use runClient.
- Kill processes by PID only (no pkill/killall).
- Only build with ≥ 4 GB free (`free -g` available column).
- Keep MIT notices (LICENSE, LICENSE-apoli, LICENSE-calio, file headers) and LGPL notices of any library code you copy (PAL, Cloth Config).
- H1 lands first; then everyone `git merge ol-integration` into their branch before continuing.

## Compile (example for H1; substitute N)
```
[ $(free -g | awk '/Mem/{print $7}') -ge 4 ] && /workspace/mod-port/ars-compile.sh /workspace/mod-port/ol-wt-h1 /tmp/port-ol-h1.lock /tmp/pcache-ol-h1 /tmp/ol-h1.log compileJava
```
## Errors per owner
```
python3 tools/owner_errors.py /tmp/ol-hN.log          # counts per owner
python3 tools/owner_errors.py /tmp/ol-hN.log H3       # error lines for H3
```

## API requests from H3 (for H1)
- **PAL stub — DONE by H3** (`io.github.ladysnake.pal`, upstream names kept: `Pal`, `AbilitySource`, `PlayerAbility`, `VanillaAbilities.ALLOW_FLYING/FLYING/INVULNERABLE/LIMIT_WORLD_MODIFICATIONS`; hooks `Pal.forget(player)` on logout, `Pal.refreshAll(player)` on game-mode change/respawn — H1 please wire). Original request: `PlayerAbilityPower` still imports `io.github.ladysnake.pal.{AbilitySource, Pal, PlayerAbility}` unchanged. Please provide a local stub with the same names: `Pal.getAbilitySource(Identifier)`, `AbilitySource.grants/grantTo/revokeFrom(Player, PlayerAbility)`, and `PlayerAbility`. It also needs the `VanillaAbilities` that the PowerFactories use. Back it with `Player.getAbilities()` plus per-source tracking and `onUpdateAbilities()`. Keep the LGPL notice if any PAL code is copied.
- **AT line — DONE by H3**: `public net.minecraft.client.gui.GuiGraphicsExtractor guiRenderState`. OverlayPower adds a custom GuiElementRenderState. Fabric API widened this field; NeoForge doesn't.
- **PowerTypes registry lookup** (aligned with H6 321bacb): `PowerTypes.prepareSharedState` uses `this.getRegistryLookup()`. H1: make `calio.data.MultiJsonDataLoader` extend NeoForge `ContextAwareReloadListener` (or expose `getRegistryLookup()`), and register via `AddServerReloadListenersEvent` so the context is injected.
- **Elytra (decided: mixin, H3)**: `apoli.mixin.ElytraDenyMixin` (RETURN of `LivingEntity#canGlide(Z)Z`) denies gliding when `PreventElytraFlightPower.allowsElytraFlight` is false. H1: do NOT add an event for it.
- **ArgumentTypeInfos mixins dropped** (apoli + origins): H1 register via `DeferredRegister<ArgumentTypeInfo<?,?>>` (Registries.COMMAND_ARGUMENT_TYPE) + `ArgumentTypeInfos.registerByClass`: `apoli:power` PowerTypeArgumentType, `apoli:power_operation` PowerOperation, `origins:origin` OriginArgumentType, `origins:layer` LayerArgumentType (all `SingletonArgumentInfo.contextFree`).
- **calio ReloadableServerResourcesMixin dropped**: H1 add `OrderedResourceListenerManager.getRegistryBasedReloadListenerProviders()` listeners in `AddServerReloadListenersEvent` using `event.getRegistryAccess()`; those providers should return `PreparableReloadListener` (Fabric `IdentifiableResourceReloadListener` is gone).
- **Component keys API used by mixins** (CCA → attachments): `PowerHolderComponent.KEY` / `ModComponents.ORIGIN` must keep `get(Entity)`, `maybeGet(Entity)`, `sync(Entity)` and `syncWith(ServerPlayer recipient, Entity owner)` (no `ComponentProvider` cast).
- **MixinSquared and mixinconstraints dropped** (the Lithium suffocation compat handler and the empty DebugContextMixin were removed).
## H4 notes / cross-owner items
- [H4] PAL stand-in now on ol-integration (2303e82); H4 PAL errors resolved.
- [H4→H6] Apoli.java:125-126 still calls Fabric `ResourceConditions.register(...)`. Replace with `ApoliResourceConditions.register(modBus);` in the mod constructor (it's now a NeoForge `DeferredRegister` of condition codecs; ids unchanged: apoli:any_namespace_loaded / apoli:all_namespaces_loaded).
- [H4 info] ElytraFlightPossibleCondition: Fabric EntityElytraEvents.CUSTOM → any glider item in any equipment slot or NeoForge GLIDING_FLIGHT attribute; ALLOW → !hasPower(PreventElytraFlightPower). Matches H3's allowsElytraFlight().
- [H4 info] ApoliResourceConditions.test accepts both `fabric:load_conditions` (`condition` field) and `neoforge:conditions` (`type` field), resolved via NeoForge's condition registry; tag-based conditions are rejected (TAGS_INVALID) since power files load before tags. Fabric-only condition ids (fabric:*) are now unknown → file skipped with an error log.
- [H4→H3 (mixins)] ElytraFlightPower: Fabric EntityElytraEvents.CUSTOM listener removed; `ElytraFlightPower.grantsElytraFlight(entity)` is the replacement predicate. The gliding hook (e.g. alongside ElytraDenyMixin, LivingEntity.canGlide / updateFallFlying) must let entities with this power start and keep gliding without an elytra. ElytraFlightPossibleCondition already uses it.
## Handovers / cross-owner notes
- 2026-10-14 (Coding): origins/Origins.java, apoli/Apoli.java, origins/OriginsClient.java moved H1 -> H6. Networking stays with H1.
  - One `@Mod("origins")` constructor (Origins) calls `Apoli.init(modBus, container)` first; OriginsClient is `@Mod(value="origins", dist=CLIENT)` and calls `origins.integration.ModMenuIntegration.register(container)` once.
  - Reload listeners via AddServerReloadListenersEvent: apoli:powers (PowerTypes, H3 setter `setRegistryLookup(event.getServerResources().getRegistryLookup())` as H3 asked), apoli:global_powers, origins:origins, origins:origin_layers, origins:badge (powers -> origins -> layers; powers -> badge).
  - Commands via RegisterCommandsEvent (OriginCommand, PowerCommand, ResourceCommand). Vanilla-registry entries (recipe serializers, data components, loot functions/conditions, triggers) via RegisterEvent.
- H6 -> H3: apoli/power/PowerTypes.java:53 still reads Fabric `ResourceLoader.REGISTRY_LOOKUP_KEY` on ol-integration; H3's setter (ol-h3 PowerTypes.java:55) replaces it; H6 calls the setter.
- H6 -> H1: calio DataObjectRegistry#getLoader() and apoli/global/GlobalPowerSetLoader must be plain PreparableReloadListeners (no Fabric IdentifiableResourceReloadListener); badge loader is registered under id origins:badge.
- H6 -> H4: apoli/util/ApoliResourceConditions must register ANY/ALL_NAMESPACES_LOADED through NeoForge's condition codec registry (Fabric ResourceConditions.register calls were dropped from Apoli).
- 2026-10-14 (Coding): moved H1 -> H6: AEA attributes (new de/dafuqs/additionalentityattributes/AdditionalEntityAttributes.java, DeferredRegister<Attribute>, generic./player. aliases kept in Apoli), calio/registry/DataObjectRegistry.java, apoli/global/GlobalPowerSetLoader.java (plain NeoForge reload listeners; ids origins:badge, apoli:global_powers, global_powers depends on apoli:powers), META-INF/neoforge.mods.toml (apoli + calio as namespace-only [[mods]] entries; Origins is the single @Mod and calls Apoli.init first).
- PAL stub stays with H3 (ol-h3). Apoli.java uses upstream io.github.ladysnake.pal names (Pal.getAbilitySource, AbilitySource, PlayerAbility, VanillaAbilities); errors expected until H3 merges.
- **PAL stub**: `PlayerAbilityPower` still imports `io.github.ladysnake.pal.{AbilitySource, Pal, PlayerAbility}` unchanged. Please provide a local stub with the same names: `Pal.getAbilitySource(Identifier)`, `AbilitySource.grants/grantTo/revokeFrom(Player, PlayerAbility)`, and `PlayerAbility`. It also needs the `VanillaAbilities` that the PowerFactories use. Back it with `Player.getAbilities()` plus per-source tracking and `onUpdateAbilities()`. Keep the LGPL notice if any PAL code is copied.
- **AT line**: `public net.minecraft.client.gui.GuiGraphicsExtractor guiRenderState`. OverlayPower adds a custom GuiElementRenderState. Fabric API widened this field; NeoForge doesn't.
- **PowerTypes registry lookup**: Fabric got it from `ResourceLoader.REGISTRY_LOOKUP_KEY`. Now call `powerTypes.setRegistryLookup(event.getRegistryAccess())` when adding the listener in `AddServerReloadListenersEvent`.
- **Elytra**: the Fabric `EntityElytraEvents.ALLOW` listener was removed. The elytra-flight check (H1 event or H5 mixin) must deny flight when `PreventElytraFlightPower.allowsElytraFlight(entity)` is false.

## H1 platform shim API (package `xyz.bluspring.originslegacy.platform`, branch ol-h1)
Entry points: `xyz.bluspring.originslegacy.OriginsLegacyMod` (`@Mod("origins")`) calls `Platform.init`, bootstraps the component keys, registers the argument types, then runs `new Calio().onInitialize()`, `new OrderedResourceListener().onInitialize()`, `new Apoli().onInitialize()`, `new Origins().onInitialize()`, and on a dedicated server `new ApoliServer().onInitializeServer()`. `OriginsLegacyClientMod` (`dist = CLIENT`) runs `CalioClient`, `ApoliClient` and `OriginsClient` `.onInitializeClient()`.
**Contract (H3 Calio, H6 Apoli/Origins/OriginsClient):** keep a public no-arg constructor and those public `onInitialize()`/`onInitializeClient()` methods. Drop `implements ModInitializer`/`ClientModInitializer` and `@Override`. Do not add your own `@Mod` class.
- `Platform`: `modBus()`, `gameBus()` (= `NeoForge.EVENT_BUS`), `isModLoaded(id)`, `getModVersion(id)` → `Optional<String>` (replaces FabricLoader `getModContainer(..).getMetadata().getVersion()`), `isClient()`, `isDedicatedServer()`, `isDevelopmentEnvironment()`, `getConfigDir()`, `getGameDir()`, `currentServer()`.
- `PlatformNetworking` (replaces PayloadTypeRegistry, ServerPlayNetworking, ClientPlayNetworking, PlayerLookup). Call these during mod construction:
  `registerClientbound(type, codec)`, `registerServerbound(type, codec)`, `registerServerReceiver(type, (payload, IPayloadContext ctx) -> ..)`, `registerClientReceiver(type, handler)` (client code).
  Send with `sendToPlayer`, `sendToAll`, `sendToTracking`, `sendToTrackingAndSelf`, `sendToDimension`, `sendToServer` (client). Lookups: `tracking(entity)`, `all(server)`, `forTrackingAndSelf(entity, consumer)`.
  Handlers run on the main thread: use `ctx.enqueueWork(..)` or nothing, not `context.server().execute`. `ctx.player()` returns a `Player`, so cast it to `ServerPlayer` on the server. `context.client()` becomes `Minecraft.getInstance()`. The Fabric login handshake (version check) is removed, because NeoForge channel versioning covers it.
- `PlatformRegistries`: `register(Registries.X, id, value)` returns the value and is registered in `RegisterEvent`. Replaces `Registry.register(BuiltInRegistries.X, ..)`, which throws on NeoForge because the registry is frozen. `registerHolder(Registries.X, id, supplier)` returns a `DeferredHolder` when a `Holder` is needed (attributes, effects). `createSimple(key)`/`createSynced(key)` replace `FabricRegistryBuilder...buildAndRegister()`. Everything must be called during mod construction.
- `PlatformReloadListeners` (server data): `registerServer(IdentifiableResourceReloadListener)`, `registerServer(id, provider -> listener, after...)`, `registerServer(id, listener, (l, lookup) -> l.setRegistryLookup(lookup), after...)`, `addOrdering(first, second)`, `onServerReload((event, lookup) -> ..)`, and `AFTER_VANILLA`. Every listener runs after vanilla. The lookup comes from `event.getServerResources().getRegistryLookup()`, because `AddServerReloadListenersEvent` has no `getRegistryAccess()`. **H6:** register PowerTypes like this: `PlatformReloadListeners.registerServer(Apoli.identifier("powers"), powerTypes, PowerTypes::setRegistryLookup)`. `SimplePreparableReloadListener`-based listeners (MultiJsonDataLoader, for example) already extend NeoForge `ContextAwareReloadListener`, so `getRegistryLookup()` is injected automatically (H3).
- `platform.IdentifiableResourceReloadListener`: same shape as Fabric's (`getFabricId()`, `getFabricDependencies()`), so only the import changes.
- `platform.event.Event` / `EventFactory.createArrayBacked(Class, Function<T[],T>)`: drop-in for Fabric `Event`/`EventFactory`, with `register` and `invoker()`. Only the import changes.
- `PalHooks` (internal) wires `Pal.forget` on logout, and `Pal.refreshAll` on respawn and on game-mode change (next tick).
- Components (CCA replacement), `platform.component`: `ComponentKey.create(id, Class, holderEntityClass, factory, RespawnCopyStrategy)` is backed by a NeoForge `AttachmentType` and registered automatically. Methods: `get(entity)`, `maybeGet(entity)`, `isPresent(entity)` (does not create), `sync(entity)` (tracking + self), `syncWith(ServerPlayer, entity)`. Implement `PlatformComponent` (`readData`/`writeData`, optional `shouldSyncWith`, `writeSyncPacket`, `applySyncPacket`), plus `ServerTickingComponent`/`ClientTickingComponent` for ticking. Auto-sync happens on login, start-tracking, respawn and dimension change, through the `origins:component_sync` payload. Copy on respawn: ALWAYS_COPY/CHARACTER use copyOnDeath, INVENTORY needs keep_inventory, NEVER_COPY never copies.
  `PowerHolderComponent.KEY` (all LivingEntity, ALWAYS_COPY) and `ModComponents.ORIGIN` (Player, CHARACTER) are defined in their own classes. **H6:** delete `Apoli implements EntityComponentInitializer`/`registerEntityComponentFactories`. They are no longer needed.
- Argument types `apoli:power`, `apoli:power_operation`, `origins:origin` and `origins:layer`: `apoli.registry.ApoliArgumentTypes` and `origins.registry.ModArgumentTypes`, both called by `OriginsLegacyMod`. H6 has nothing to do here.
- `ModLoot.registerLootTables()` now uses `LootTableLoadEvent` (minecraft namespace only). H6 still calls it from Origins.

### H1 cross-owner notes
- H6 (Apoli.java/Origins.java/OriginsClient.java): `ModPackets.init()`, `ModPacketsC2S.register()` and `ModPacketsS2C.register()` keep their names and are now backed by PlatformNetworking. Commands: `NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> PowerCommand.register(e.getDispatcher()) ...)`. `ServerLifecycleEvents.SERVER_STARTED` becomes `ServerStartedEvent`. `END_DATA_PACK_RELOAD` becomes `OnDatapackSyncEvent` with `getPlayer()==null`, or a `PlatformReloadListeners.onServerReload` hook. ResourceConditions go to NeoForge `ICondition` (register the MapCodec via `PlatformRegistries.register(NeoForgeRegistries.Keys.CONDITION_CODECS, ...)`). Apoli VERSION: `Platform.getModVersion("origins")`. The ModMenu config screen becomes `container.registerExtensionPoint(IConfigScreenFactory.class, ...)` in client code, using `Platform.container()`. `Registry.register(BuiltInRegistries.X ..)` calls in Apoli/Origins must move to `PlatformRegistries.register(Registries.X ..)`.
- H6 AEA: attribute registration belongs to H6. Use `PlatformRegistries.registerHolder(Registries.ATTRIBUTE, ..)` and `EntityAttributeModificationEvent` on `Platform.modBus()`. The three AEA-using powers need the import swapped.
- H3 Calio: Calio's static `Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE/TRIGGER_TYPES, ..)` must use `PlatformRegistries.register(Registries.X, ..)`. `CalioNetworking` should use `PlatformNetworking.registerClientbound`. `OrderedResourceListener` (FabricLoader entrypoints) should be invoked directly; the entrypoint key no longer exists. Reload listeners should use `PlatformReloadListeners`.
- H4: `apoli/power/factory/action/meta/DelayAction` creates its own `new Scheduler()` besides `Apoli.SCHEDULER` (H4/H6 to dedupe if Scheduler must be a singleton).
- Known gap: attachment load order is not guaranteed. If ORIGIN deserialises before `apoli:powers`, `PlayerOriginComponent.readData` touches a fresh PowerHolder, which the saved one then replaces. That is harmless because the powers are saved too, but watch for it in testing.
- [H4] AEA stand-in (de.dafuqs.additionalentityattributes, now H4) uses AEA 2.2.4 values/attachment; WATER_SPEED = NeoForge SWIM_SPEED (alias additionalentityattributes:water_speed -> neoforge:swim_speed). LAVA_SPEED and LAVA_VISIBILITY are applied by apoli/mixin/LavaAttributesMixin + LavaFogMixin (handed H3→H4, registered in apoli.mixins.json), using AEA 2.2.4's exact formulas.
- [H4→H6] Apoli.java:64: `public static final Scheduler SCHEDULER = new Scheduler();` -> `public static final Scheduler SCHEDULER = Scheduler.get();` (shared singleton; the constructor is deprecated but still shares the one queue/listener, so nothing breaks before the change). origins/util/Scheduler was deleted (no users).
- [H6/H3] Apoli.java:194 calls `powerTypes.setRegistryLookup(...)`, which PowerTypes (H3) doesn't have on ol-integration cb79695 (the only non-H1 error left).
- 2026-10-14 (Coding): AEA (de/dafuqs/additionalentityattributes/**) is H4's now (Apoli.init still calls AdditionalEntityAttributes.register(modBus)).
- H6 on H1 shim: Origins/OriginsClient have no @Mod. Shim calls `Origins.init(IEventBus, ModContainer)` (calls Apoli.init first) and client-only `OriginsClient.init(IEventBus, ModContainer)`. Registry entries go through PlatformRegistries; reload listeners go through PlatformReloadListeners (powers with PowerTypes::setRegistryLookup; global_powers, origins, badge after powers; origin_layers after origins); game events go through Platform.gameBus().
- 2026-10-14 (Coding): AEA (de/dafuqs/additionalentityattributes/**) is H4's now (Apoli.init still calls AdditionalEntityAttributes.register(modBus)).
- H6 on H1 shim: Origins/OriginsClient have no @Mod. Shim calls `Origins.init(IEventBus, ModContainer)` (calls Apoli.init first) and client-only `OriginsClient.init(IEventBus, ModContainer)`. Registry entries go through PlatformRegistries; reload listeners go through PlatformReloadListeners (powers with PowerTypes::setRegistryLookup; global_powers, origins, badge after powers; origin_layers after origins); game events go through Platform.gameBus().
- [H4] AEA stand-in (de.dafuqs.additionalentityattributes, now H4) uses AEA 2.2.4 values/attachment; WATER_SPEED = NeoForge SWIM_SPEED (alias additionalentityattributes:water_speed -> neoforge:swim_speed). LAVA_SPEED and LAVA_VISIBILITY are applied by apoli/mixin/LavaAttributesMixin + LavaFogMixin (handed H3→H4, registered in apoli.mixins.json), using AEA 2.2.4's exact formulas.
- [H4→H6] Apoli.java:64: `public static final Scheduler SCHEDULER = new Scheduler();` -> `public static final Scheduler SCHEDULER = Scheduler.get();` (shared singleton; the constructor is deprecated but still shares the one queue/listener, so nothing breaks before the change). origins/util/Scheduler was deleted (no users).
- [H6/H3] Apoli.java:194 calls `powerTypes.setRegistryLookup(...)`, which PowerTypes (H3) doesn't have on ol-integration cb79695 (the only non-H1 error left).

## Calio (H3) — reload-listener helper for H6
- `io.github.apace100.calio.resource.CalioReloadListeners`:
  - `onAddServerReloadListeners(AddServerReloadListenersEvent)`: call it from a game-bus listener. It adds every Calio-ordered server listener after vanilla, in dependency order, including the registry-based ones created from `event.getServerResources().getRegistryLookup()`.
  - `onAddClientReloadListeners(AddClientReloadListenersEvent)`: call it from a mod-bus listener, client only.
  - Call each one exactly once per event. Don't also register the same listeners through PlatformReloadListeners.
- `OrderedResourceListener.registerInitializer(OrderedResourceListenerInitializer)` replaces the Fabric `calio:ordered-resource-listener` entrypoint. Apoli and Origins (H6) must call it in their `onInitialize` if they still use `registerResourceListeners(manager)`. Initializers run lazily on the first reload event.
- Registry-based providers now take `Function<HolderLookup.Provider, ? extends PreparableReloadListener>` (`OrderedResourceListenerManager.registerWithRegistries`).
- `MultiJsonDataLoader` needs no base-class change. NeoForge patches `SimplePreparableReloadListener` to extend `ContextAwareReloadListener`, so `getRegistryLookup()` already works in `prepareSharedState`.
- Calio's data components and trigger go through `PlatformRegistries.register`. Its payload goes through `PlatformNetworking.registerClientbound` / `registerClientReceiver`.
## Elytra and lava (H3 mixins)
- Gliding grant and deny live only in `ElytraDenyMixin`; deny wins over grant. Lava speed and fog use `ApoliAttributes.LAVA_SPEED` / `LAVA_VISIBILITY` through `LavaAttributesMixin` and `LavaFogMixin`.
- Note for H4 and H6: `de.dafuqs.additionalentityattributes.AdditionalEntityAttributes` (H6) also defines LAVA_SPEED and LAVA_VISIBILITY. Only the ApoliAttributes ones have gameplay hooks, so the two need deduplicating.
- H6: CalioReloadListeners.onAddServerReloadListeners on Platform.gameBus() in Origins.init; onAddClientReloadListeners on modBus in OriginsClient.init (client-only). Apoli/Origins use no OrderedResourceListenerInitializer, so no registerInitializer call.
- [H4] AEA stand-in (de.dafuqs.additionalentityattributes, now H4) uses AEA 2.2.4 values/attachment; WATER_SPEED = NeoForge SWIM_SPEED (alias additionalentityattributes:water_speed -> neoforge:swim_speed). LAVA_SPEED and LAVA_VISIBILITY applied via H4 mixins (additionalentityattributes.mixins.json).
- [H4→H6] neoforge.mods.toml: add
      [[mixins]]
          config="additionalentityattributes.mixins.json"
  Without it the lava speed / lava fog hooks do not load.
- [H4→H6] Apoli.java:64: `public static final Scheduler SCHEDULER = new Scheduler();` -> `public static final Scheduler SCHEDULER = Scheduler.get();` (shared singleton; the constructor is deprecated but still shares the one queue/listener, so nothing breaks before the change). origins/util/Scheduler was deleted (no users).
- [H6/H3] Apoli.java:194 calls `powerTypes.setRegistryLookup(...)`, which PowerTypes (H3) doesn't have on ol-integration cb79695 (the only non-H1 error left).

## Single @Mod contract (decided: shim owns it), round 2
- The only `@Mod` classes are `xyz.bluspring.originslegacy.OriginsLegacyMod` (common) and `OriginsLegacyClientMod` (`dist = CLIENT`).
- Common order: `Platform.init`, component keys, argument types, `new Calio().onInitialize()` (H3), `new OrderedResourceListener().onInitialize()` (H3), then **`Origins.init(IEventBus modBus, ModContainer container)` (H6, once; it calls `Apoli.init` itself)**, then `new ApoliServer().onInitializeServer()` on a dedicated server only.
- Client order: `new CalioClient().onInitializeClient()` (H3), `new ApoliClient().onInitializeClient()` (H1; idempotent; registers Apoli S2C receivers and the active-power key tick), then **`OriginsClient.init(IEventBus modBus, ModContainer container)` (H6, once; it also registers the config screen)**.
- H6: turn the `Origins`/`OriginsClient` constructors into `public static void init(IEventBus, ModContainer)` and remove `@Mod`. A ready patch is at `/tmp/ol-h1-to-h6-mod.diff` (`git apply` on top of ol-integration 55007c7). Don't call `ApoliClient` from OriginsClient; if you do, the guard makes it harmless.
- Until H6 lands this, ol-h1 shows 2 compile errors in `OriginsLegacyMod`/`OriginsLegacyClientMod` (missing `init`), and running it would load the mod three times.
- Gliding grant and deny live only in `ElytraDenyMixin`; deny wins over grant.
- [H4] Lava hooks deduplicated: LavaAttributesMixin/LavaFogMixin are H4 and use AdditionalEntityAttributes (ApoliAttributes deleted).
- H6: PowerTypes registered via PlatformReloadListeners.registerServer(apoli:powers, lookup -> new PowerTypes()); no setRegistryLookup (lookup injected during reload, per H1). Apoli.SCHEDULER = Scheduler.get().
- H6: PowerTypes registered via PlatformReloadListeners.registerServer(apoli:powers, lookup -> new PowerTypes()); no setRegistryLookup (lookup injected during reload, per H1). Apoli.SCHEDULER = Scheduler.get().
- Gliding grant and deny live only in `ElytraDenyMixin`; deny wins over grant.
- [H4] Lava hooks deduplicated: LavaAttributesMixin/LavaFogMixin are H4 and use AdditionalEntityAttributes (ApoliAttributes deleted).

## Runtime crashes found by H3 (server test 2026-10-14 00:59, ol-h3 8566f9e; log /tmp/ol-h3-server.log)
- **Owners H1 (`origins/registry/ModBlocks`) and H6 (`origins/content/TemporaryCobwebBlock`): eager block construction after the registries freeze.**
  `IllegalStateException: Registry is already frozen` at `TemporaryCobwebBlock.<init>(TemporaryCobwebBlock.java:21)` <- `ModBlocks.<clinit>(ModBlocks.java:20)` <- `Origins.init(Origins.java:107)` <- `OriginsLegacyMod.<init>(OriginsLegacyMod.java:34)`.
  Then `NoClassDefFoundError: Could not initialize class ModBlocks`.
  Fix: make ModBlocks/ModItems use DeferredRegister.Blocks/Items (or PlatformRegistries.registerHolder) with lazy suppliers. Use `BlockBehaviour.Properties.setId(ResourceKey)`, which 26.x requires, and don't construct blocks or items in `<clinit>`. `Registry.register(BuiltInRegistries.BLOCK/ITEM, ...)` at lines 32/34 must also go.
- **Fixed by H3:**
  - Mixin 0-target injections on the NeoForge-patched jar: WaterBreathingMixin$UpdateAir, LivingEntityMixin.modifySlipperiness, ItemStackMixinClient tooltip, ServerPlayerEntityMixin sleep.
  - Calio's DontOverwriteAttrModsMixin was removed (its @Local was invalid and the mixin did nothing).
  - OverlayPower loaded client classes on the dedicated server; its rendering is split into OverlayPowerRenderer.
- **Info (H6):** the NeoForge OnlyInWarningsHandler logs ERROR lines for @OnlyIn on ModMenuIntegration (apoli/origins), GameHudRender, PowerHudRenderer.render, Origin/ConditionedOrigin.read, Badge.getTooltipComponents and EnderianPearlEntity.<init>. NeoForge no longer strips @OnlyIn members, so it isn't fatal, but it is noise for the ERROR checks in the tests.
- 2026-10-14 (Coding, shared fix by H6): origins/registry ModBlocks/ModItems/ModEntities reassigned H1 -> H6. Fixes "Registry is already frozen" (TemporaryCobwebBlock.<init>:21 <- ModBlocks.<clinit>:20): all three use DeferredRegister + setId(ResourceKey) from the register id, registered on the mod bus in Origins.init; fields are DeferredHolders (use .get()).
- H6: removed all @OnlyIn (NeoForge OnlyInWarningsHandler ERROR lines): origins+apoli ModMenuIntegration, GameHudRender, PowerHudRenderer, Origin.read, OriginLayer.ConditionedOrigin.read, Badge.getTooltipComponents, EnderianPearlEntity ctor. These are only reached from client code paths.
- 2026-10-14 H6 (for H3, by request): apoli/mixin/ServerPlayerInteractionManagerMixin: NeoForge-patched destroyBlock has one boolean (canDestroy=canHarvestBlock) before mineBlock, so modifyEffectiveTool ordinal 1->0; actionOnBlockBreak locals now removingBlock=ordinal 1 (changed), hasCorrectTool=ordinal 0 (canDestroy).
- H6 (for H3): apoli/mixin/PhantomSpawnerMixin: NeoForge tick() has only one hasSkyLight(); cachePlayerEntity now injects at ServerPlayer.isSpectator() (ordinal 0) inside the player loop.
