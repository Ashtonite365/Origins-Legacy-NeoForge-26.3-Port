# Origins: Legacy - NeoForge 26.3 Port

> **Unofficial** NeoForge 26.3 (Minecraft 26.3) port. Not affiliated with or endorsed by the original authors.

## Credits

This is an **unofficial port** of **[Origins: Legacy](https://modrinth.com/mod/origins-legacy)** by **BluSpring**, which is a forward port of **Origins** by **apace100**.

- Original mod: https://modrinth.com/mod/origins-legacy (CurseForge: https://www.curseforge.com/projects/1429195)
- Original source: https://github.com/BluSpring/Origins-Legacy (also https://github.com/BluSpring/apoli-legacy and https://github.com/BluSpring/calio-legacy)
- Origins / Apoli / Calio by apace100: https://github.com/apace100/origins-fabric, https://github.com/apace100/apoli, https://github.com/apace100/calio
- NeoForge 26.3 port by Ashtonite365

## Licence

Origins: Legacy, Apoli-Legacy and Calio-Legacy are licensed under the **MIT License**, (c) 2020-2021 apace100. This port is released under the same MIT licence. The original licence files are in the jar (`LICENSE`, `LICENSE-apoli`, `LICENSE-calio`).


## How this port differs

- Ported from **Origins: Legacy 1.12.19 (Fabric, Minecraft 26.3)** to **NeoForge 26.3.0.76+ for Minecraft 26.3**.
- **Unofficial and beta:** expect bugs and back up your worlds.
- Apoli-Legacy 2.12.13 and Calio-Legacy 1.11.4 are bundled in a single jar.
- The Fabric-only libraries are replaced with NeoForge equivalents: Cardinal Components becomes NeoForge data attachments, and PlayerAbilityLib and Additional Entity Attributes are reimplemented internally. Requires **Cloth Config 26.3.159+neoforge** or newer. ModMenu support is replaced by NeoForge's config screen.
- The mod ids are changed to **`origins_legacy_port263`**, **`apoli_legacy_port263`** and **`calio_legacy_port263`**. Content still uses the `origins:`, `apoli:` and `calio:` namespaces, so origin/power datapacks keep working. Don't install it alongside another Origins build.

## Not affiliated

This port is **not affiliated with or endorsed by** BluSpring or apace100. Please don't report bugs from this port to them.

## Building

Original licence files: `LICENSE`, `LICENSE-apoli`, `LICENSE-calio`.

`./gradlew build` (JDK required; the jar lands in `build/libs/`).
