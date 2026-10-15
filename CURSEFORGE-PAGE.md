# Origins: Legacy NeoForge 26.3 Port

> **Unofficial, beta.** This is a community port. It is **not** made or supported by BluSpring or apace100. Please do **not** report bugs from this port to the original authors.

An unofficial NeoForge port of **Origins: Legacy 1.12.19** for **Minecraft 26.3**. It bundles Apoli-Legacy 2.12.13 and Calio-Legacy 1.11.4 in a single jar.

- **Version:** 1.12.19-port.1-beta (beta: expect bugs, back up your worlds)
- **Mod loader:** NeoForge **26.3.0.76** or newer
- **Minecraft:** 26.3
- **Required dependency:** [Cloth Config](https://www.curseforge.com/minecraft/mc-mods/cloth-config) **26.3.159+neoforge** or newer
- **Environment:** required on both client and server

## Credits

- **Origins** by **apace100**, the original mod: https://github.com/apace100/origins-fabric
- **Apoli** and **Calio** by **apace100**: https://github.com/apace100/apoli and https://github.com/apace100/calio
- **Origins: Legacy** (with Apoli-Legacy and Calio-Legacy) by **BluSpring**, the forward-ported Fabric version this port is based on:
  - https://github.com/BluSpring/Origins-Legacy (branch `versions/26.3`)
  - https://github.com/BluSpring/apoli-legacy
  - https://github.com/BluSpring/calio-legacy
- **NeoForge 26.3 port** by **Ashtonite365**

## Source and licence

Origins, Apoli and Calio are MIT-licensed, (c) 2020-2021 apace100. This port is released under the same MIT licence. The original MIT licence files are included in the jar as `LICENSE`, `LICENSE-apoli` and `LICENSE-calio`.

- Original source: https://github.com/BluSpring/Origins-Legacy

## Compatibility notes

- Mod ids are **`origins_legacy_port263`**, **`apoli_legacy_port263`** and **`calio_legacy_port263`**, so they do not clash with the official `origins`, `apoli` or `calio` mod ids.
- All content still uses the original **`origins:`**, **`apoli:`** and **`calio:`** namespaces. Existing datapacks, origin/power JSON and commands keep working unchanged.
- Loading alongside another Origins / Apoli / Calio build will not cause a duplicate-mod error, but it is **not supported**: both use the same `origins:` / `apoli:` / `calio:` content namespaces, so their registries and data will collide. Use only one Origins build at a time.
