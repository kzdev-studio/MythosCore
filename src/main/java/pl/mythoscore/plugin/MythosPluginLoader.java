package pl.mythoscore.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPluginLoader;
import pl.mythoscore.MythosCoreMod;
import pl.mythoscore.server.MythosServer;

import java.io.File;
import java.util.*;

@SuppressWarnings("removal")
public class MythosPluginLoader {

    private final File pluginsFolder;
    private final List<Plugin> enabledPlugins = new ArrayList<>();

    public MythosPluginLoader() {
        this.pluginsFolder = new File("plugins");
        if (!pluginsFolder.exists() && !pluginsFolder.mkdirs()) {
            MythosCoreMod.LOGGER.warn("Nie udało się utworzyć folderu plugins.");
        }
    }

    public void loadPlugins() {
        File[] files = pluginsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files == null || files.length == 0) {
            MythosCoreMod.LOGGER.info("Brak pluginów w folderze 'plugins/'.");
            return;
        }

        MythosCoreMod.LOGGER.info("Znaleziono {} plików JAR w 'plugins/'. Analizuję zależności...", files.length);

        JavaPluginLoader sharedLoader = new JavaPluginLoader(Bukkit.getServer());
        Map<String, File> fileByName = new LinkedHashMap<>();
        Map<String, PluginDescriptionFile> descByName = new LinkedHashMap<>();

        for (File file : files) {
            try {
                PluginDescriptionFile desc = sharedLoader.getPluginDescription(file);
                if (desc == null || desc.getName() == null || desc.getName().isBlank()) {
                    MythosCoreMod.LOGGER.warn("Pominięto {}: brak poprawnego plugin.yml.", file.getName());
                    continue;
                }
                String key = desc.getName();
                fileByName.put(key, file);
                descByName.put(key, desc);
            } catch (InvalidDescriptionException e) {
                MythosCoreMod.LOGGER.error("Nieprawidłowy plugin.yml w {}: {}", file.getName(), e.getMessage());
            } catch (Throwable t) {
                MythosCoreMod.LOGGER.error("Błąd odczytu opisu {}: {}", file.getName(), t.getMessage());
            }
        }

        List<String> loadOrder = resolveLoadOrder(descByName);
        List<Plugin> loaded = new ArrayList<>();

        // 1. Faza ładowania + onLoad()
        for (String name : loadOrder) {
            File file = fileByName.get(name);
            PluginDescriptionFile desc = descByName.get(name);
            if (!hasRequiredDependencies(desc, fileByName.keySet())) {
                MythosCoreMod.LOGGER.error("Pominięto {} — brak wymaganych zależności: {}", name, desc.getDepend());
                continue;
            }
            try {
                MythosCoreMod.LOGGER.info("-> Ładowanie: {} v{} ({})", name, desc.getVersion(), file.getName());
                Plugin plugin = sharedLoader.loadPlugin(file);
                MythosServer.registerLoadedPlugin(plugin);
                registerCommands(plugin, desc);
                try {
                    plugin.onLoad();
                } catch (Throwable t) {
                    MythosCoreMod.LOGGER.error("Błąd w onLoad() dla {}: {}", name, t.getMessage(), t);
                }
                loaded.add(plugin);
            } catch (Throwable t) {
                MythosCoreMod.LOGGER.error("Nie udało się załadować pluginu {}: {}", file.getName(), t.getMessage(), t);
            }
        }

        // 2. Faza uruchamiania (enablePlugin ustawia isEnabled=true i wywołuje onEnable)
        for (Plugin plugin : loaded) {
            try {
                MythosCoreMod.LOGGER.info("   [ENABLE] Uruchamianie {}...", plugin.getName());
                Bukkit.getPluginManager().enablePlugin(plugin);
                if (plugin.isEnabled()) {
                    enabledPlugins.add(plugin);
                }
                MythosCoreMod.LOGGER.info("   [SUCCESS] {} uruchomiony (enabled={})!", plugin.getName(), plugin.isEnabled());
            } catch (Throwable t) {
                MythosServer.setPluginEnabledState(plugin, false);
                MythosCoreMod.LOGGER.error("   [ERROR] {} wyłożył się w onEnable(): {}", plugin.getName(), t.getMessage(), t);
            }
        }
    }

    public void disablePlugins() {
        List<Plugin> reversed = new ArrayList<>(enabledPlugins);
        Collections.reverse(reversed);
        for (Plugin plugin : reversed) {
            try {
                if (plugin.isEnabled()) {
                    Bukkit.getPluginManager().disablePlugin(plugin);
                }
            } catch (Throwable t) {
                MythosCoreMod.LOGGER.error("Błąd wyłączania {}: {}", plugin.getName(), t.getMessage());
            }
        }
        enabledPlugins.clear();
    }

    private void registerCommands(Plugin plugin, PluginDescriptionFile desc) {
        List<org.bukkit.command.Command> parsed = org.bukkit.command.PluginCommandYamlParser.parse(plugin);
        if (parsed != null && !parsed.isEmpty()) {
            MythosServer.getCommandMap().registerAll(plugin.getName(), parsed);
            return;
        }
        if (desc.getCommands() == null) return;
        for (String cmdName : desc.getCommands().keySet()) {
            MythosPluginCommand command = new MythosPluginCommand(cmdName, plugin);
            MythosServer.getCommandMap().register(plugin.getName(), command);
        }
    }

    private boolean hasRequiredDependencies(PluginDescriptionFile desc, Set<String> available) {
        if (desc.getDepend() == null || desc.getDepend().isEmpty()) return true;
        Set<String> lowerAvailable = new HashSet<>();
        for (String a : available) lowerAvailable.add(a.toLowerCase(Locale.ROOT));
        for (String req : desc.getDepend()) {
            if (!lowerAvailable.contains(req.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private List<String> resolveLoadOrder(Map<String, PluginDescriptionFile> descByName) {
        Map<String, String> canonicalByLower = new HashMap<>();
        for (String name : descByName.keySet()) {
            canonicalByLower.put(name.toLowerCase(Locale.ROOT), name);
        }

        Map<String, Set<String>> deps = new LinkedHashMap<>();
        for (String name : descByName.keySet()) {
            deps.put(name, new LinkedHashSet<>());
        }

        for (Map.Entry<String, PluginDescriptionFile> e : descByName.entrySet()) {
            String plugin = e.getKey();
            PluginDescriptionFile d = e.getValue();
            addDeps(deps.get(plugin), d.getDepend(), canonicalByLower);
            addDeps(deps.get(plugin), d.getSoftDepend(), canonicalByLower);
            if (d.getLoadBefore() != null) {
                for (String target : d.getLoadBefore()) {
                    String canon = canonicalByLower.get(target.toLowerCase(Locale.ROOT));
                    if (canon != null && !canon.equals(plugin)) {
                        deps.get(canon).add(plugin);
                    }
                }
            }
        }

        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        for (String name : descByName.keySet()) {
            visitTopo(name, deps, visited, visiting, order);
        }
        return order;
    }

    private void addDeps(Set<String> target, List<String> raw, Map<String, String> canonicalByLower) {
        if (raw == null) return;
        for (String dep : raw) {
            String canon = canonicalByLower.get(dep.toLowerCase(Locale.ROOT));
            if (canon != null) target.add(canon);
        }
    }

    private void visitTopo(String node, Map<String, Set<String>> deps, Set<String> visited, Set<String> visiting, List<String> order) {
        if (visited.contains(node)) return;
        if (!visiting.add(node)) return; // break cycle safely
        for (String dep : deps.getOrDefault(node, Set.of())) {
            if (!dep.equals(node)) {
                visitTopo(dep, deps, visited, visiting, order);
            }
        }
        visiting.remove(node);
        visited.add(node);
        order.add(node);
    }
}
