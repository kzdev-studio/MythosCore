# MythosCore

Hybrydowy silnik serwerowy **Minecraft 1.20.1 (Fabric + Spigot / Paper API)** tworzony przez **KZDev Studio**.

Pozwala uruchamiać jednocześnie mody **Fabric** oraz pluginy **Bukkit / Spigot / Paper** z internetu.

## Aktualne możliwości (v1.0.0-alpha)
- Ładowanie zewnętrznych pluginów z folderu `plugins/` ze wspólnym `JavaPluginLoader` i sortowaniem zależności (`depend`, `softdepend`, `loadbefore`)
- Pełna obsługa `PluginManager`, `ServicesManager` (w tym wbudowane **Vault Economy & Permissions**), `BukkitScheduler` (zadania sync, async, timer, anulowanie) oraz `Messenger`
- Mapowanie podstawowych obiektów Bukkit na Fabric: `Server`, `World`, `Player`, `OfflinePlayer`, `Location`, `Inventory`, `ItemStack`, `BossBar`, `Scoreboard`
- Mixiny wspierające kompatybilność klas i zdarzeń (`PluginClassLoaderMixin`, `InventoryClickMixin`, `CommandPacketMixin`, `DisplayEntityMixin`, `TextDisplayMixin`)

## Budowanie z kodu źródłowego
Wymagana **Java 21**:
```bash
./gradlew build
```
Gotowy plik moda/silnika zostanie wygenerowany w `build/libs/`.

## Jak będziemy rozwijać projekt?
Projekt rozwijamy **etapami (iteracyjnie)**:
1. Kolejne poprawki API Bukkit/Spigot i testy z popularnymi pluginami (np. `KZDevRegister`, `Vault`, `LuckPerms`, `EssentialsX`, `WorldEdit`) trafiają regularnie jako commity na gałąź `main`.
2. Stabilniejsze kamienie milowe publikujemy w zakładce **Releases** jako gotowe pliki `.jar`.

## Autor
**KZDev Studio** — [kzdev.pl](https://kzdev.pl) | [Discord](https://discord.gg/xQaw7tvkj4)
