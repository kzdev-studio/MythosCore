<div align="center">

<img src="assets/logo.svg" width="110" alt="MythosCore Logo" />

# MythosCore

**Hybrydowy silnik serwerowy Minecraft 1.20.1 (Fabric + Spigot / Paper API) tworzony przez KZDev Studio.**  
Uruchamiaj jednocześnie mody **Fabric** oraz pluginy **Bukkit / Spigot / Paper** w jednym procesie JVM na Javie 21.

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-2ea44f?style=flat-square&logo=minecraft&logoColor=white)
![Loader](https://img.shields.io/badge/Fabric-0.16.14-dbd0b4?style=flat-square)
![API](https://img.shields.io/badge/Spigot%20API-1.20.1--R0.1-ed8106?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-5382a1?style=flat-square&logo=openjdk&logoColor=white)
![Build](https://img.shields.io/github/actions/workflow/status/kzdev-studio/MythosCore/build.yml?branch=main&style=flat-square&label=build)
![Version](https://img.shields.io/badge/version-v1.0.0--alpha-6c8cff?style=flat-square)

[Szybki start](#szybki-start) • [Funkcje](#funkcje) • [Kompatybilność API](#kompatybilność-api) • [Budowanie](#budowanie-ze-źródła) • [Społeczność](#społeczność)

</div>

---

## Szybki start

1. Zainstaluj **Minecraft 1.20.1** z **Fabric Loader `0.16.14+`**, **Fabric API `0.92.6+1.20.1`** oraz **Java 21**.
2. Pobierz plik `MythosCore-*.jar` z zakładki **Releases** (lub z artefaktów **GitHub Actions**) i wrzuć go do katalogu `mods/`.
3. Umieść pluginy Bukkit/Spigot w katalogu `plugins/` i uruchom serwer.

---

## Funkcje

| Obszar | Zakres implementacji |
| :--- | :--- |
| **Plugin Loader** | Wspólny `JavaPluginLoader`, sortowanie zależności (`depend`, `softdepend`, `loadbefore`), `PluginCommandYamlParser`, izolacja wyjątków w `onLoad()` / `onEnable()`. |
| **Rdzeń Bukkit** | `MythosServer`, `MythosScheduler` (sync / async / timer na `END_SERVER_TICK`), `MythosEventManager`, `MythosCommandMap` z aliasami i `tabComplete`. |
| **Vault wbudowany** | Natywna rejestracja `Economy` oraz `Permission` w `ServicesManager`. |
| **Świat i byty** | `MythosPlayer`, `MythosEntity`, `MythosWorld`, `MythosLocation`, `MythosBlock`, `MythosBlockState`, `MythosChunk`, `MythosInventory`, `MythosItemStack`, `MythosBossBar`, `Scoreboard` / `Team`. |
| **Bezpieczeństwo modów** | `MythosMaterials` z bezpiecznym mapowaniem `Material`, `NamespacedKey` i `EntityType` dla zmodyfikowanych bloków, przedmiotów i mobów Fabric. |
| **Uprawnienia** | Integracja `PermissibleBase` (`PermissionAttachment`, `addAttachment`, `removeAttachment`, `isPermissionSet`) z `MythosPermissionManager`. |

---

## Kompatybilność API

Zaimplementowane zdarzenia i mechaniki pomostowe:

- **Gracz i czat:** `PlayerJoinEvent`, `PlayerQuitEvent`, `PlayerMoveEvent`, `AsyncPlayerChatEvent`, `PlayerCommandPreprocessEvent`, `PlayerInteractEvent` (`RIGHT_CLICK_BLOCK`, `RIGHT_CLICK_AIR`).
- **Świat i walka:** `BlockBreakEvent`, `BlockPlaceEvent`, `EntityDamageEvent`, `EntityDamageByEntityEvent`.
- **GUI i pakiety:** `InventoryClickEvent`, `InventoryCloseEvent`, `CommandPacketMixin`, `PluginClassLoaderMixin`, `DisplayEntityMixin`, `TextDisplayMixin`.
- **Komendy i Tab-Complete:** Integracja komend pluginowych z drzewem **Brigadier** i sugestiami argumentów.

---

## Budowanie ze źródła

Wymagana **Java 21**:

```bash
git clone https://github.com/kzdev-studio/MythosCore.git
cd MythosCore
./gradlew build
```

Gotowy plik moda/silnika zostanie wygenerowany w `build/libs/`.

---

## Społeczność

Tworzone przez **KZDev Studio** — [kzdev.pl](https://kzdev.pl) | [Discord](https://discord.gg/xQaw7tvkj4)
