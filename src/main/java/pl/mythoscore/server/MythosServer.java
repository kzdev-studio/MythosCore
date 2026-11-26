package pl.mythoscore.server;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.UnsafeValues;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import pl.mythoscore.MythosCoreMod;
import pl.mythoscore.entity.MythosBossBar;
import pl.mythoscore.entity.MythosInventory;
import pl.mythoscore.entity.MythosPlayer;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MythosServer {

    private static final MythosCommandMap commandMap = new MythosCommandMap();
    private static final Map<Class<?>, Object> registeredServices = new HashMap<>();
    private static final Map<Class<?>, RegisteredServiceProvider<?>> serviceRegistrations = new java.util.concurrent.ConcurrentHashMap<>();
    private static final List<Plugin> loadedPlugins = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static final Map<String, Plugin> pluginsByName = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, org.bukkit.permissions.Permission> permissions = new java.util.concurrent.ConcurrentHashMap<>();

    public static void registerLoadedPlugin(Plugin plugin) {
        if (plugin == null) return;
        if (!loadedPlugins.contains(plugin)) {
            loadedPlugins.add(plugin);
        }
        if (plugin.getName() != null) {
            pluginsByName.put(plugin.getName().toLowerCase(java.util.Locale.ROOT), plugin);
        }
    }

    public static void setPluginEnabledState(Plugin plugin, boolean enabled) {
        if (plugin instanceof org.bukkit.plugin.java.JavaPlugin jp) {
            try {
                java.lang.reflect.Method m = org.bukkit.plugin.java.JavaPlugin.class.getDeclaredMethod("setEnabled", boolean.class);
                m.setAccessible(true);
                m.invoke(jp, enabled);
            } catch (Throwable t) {
                MythosCoreMod.LOGGER.warn("Nie udało się ustawić stanu enabled={} dla {}: {}", enabled, plugin.getName(), t.getMessage());
            }
        }
    }

    public static final Plugin DUMMY_PLUGIN = (Plugin) Proxy.newProxyInstance(
            Plugin.class.getClassLoader(),
            new Class<?>[]{Plugin.class},
            (proxy, method, args) -> {
                if (method.getName().equals("getName")) return "MythosCore";
                if (method.getName().equals("getLogger")) return Logger.getLogger("MythosCore");
                if (method.getName().equals("isEnabled")) return true;
                if (method.getName().equals("getDescription")) return null;
                return getSafeDefault(method.getReturnType());
            }
    );

    public static MythosCommandMap getCommandMap() {
        return commandMap;
    }

    public static void init(MythosScheduler scheduler, MythosEventManager eventManager) {
        Server serverInstance = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class<?>[]{Server.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("getScheduler")) return scheduler.createScheduler();
                    if (methodName.equals("getLogger")) return Logger.getLogger("MythosCore");
                    if (methodName.equals("getCommandMap")) return commandMap;

                    if (methodName.equals("dispatchCommand")) {
                        return commandMap.dispatch((CommandSender) args[0], (String) args[1]);
                    }

                    // --- SCOREBOARD MANAGER ---
                    if (methodName.equals("getScoreboardManager")) {
                        return (org.bukkit.scoreboard.ScoreboardManager) Proxy.newProxyInstance(
                                org.bukkit.scoreboard.ScoreboardManager.class.getClassLoader(),
                                new Class<?>[]{org.bukkit.scoreboard.ScoreboardManager.class},
                                (p, m, a) -> {
                                    if (m.getName().equals("getMainScoreboard") || m.getName().equals("getNewScoreboard")) {
                                        return (org.bukkit.scoreboard.Scoreboard) Proxy.newProxyInstance(
                                                org.bukkit.scoreboard.Scoreboard.class.getClassLoader(),
                                                new Class<?>[]{org.bukkit.scoreboard.Scoreboard.class},
                                                (sp, sm, sa) -> {
                                                    if (MythosCoreMod.MC_SERVER == null) {
                                                        MythosCoreMod.LOGGER.warn("Scoreboard: MC_SERVER jest null!");
                                                        return null;
                                                    }
                                                    net.minecraft.scoreboard.ServerScoreboard board = MythosCoreMod.MC_SERVER.getScoreboard();
                                                    String sName = sm.getName();

                                                    // --- TEAMY ---
                                                    if (sName.equals("registerNewTeam")) {
                                                        String teamName = (String) sa[0];
                                                        net.minecraft.scoreboard.Team team = board.getTeam(teamName);
                                                        if (team == null) {
                                                            team = board.addTeam(teamName);
                                                            MythosCoreMod.LOGGER.info("Scoreboard: utworzono team {}", teamName);
                                                        }
                                                        return wrapTeam(board, team, teamName);
                                                    }

                                                    if (sName.equals("getTeam")) {
                                                        String teamName = (String) sa[0];
                                                        net.minecraft.scoreboard.Team team = board.getTeam(teamName);
                                                        if (team == null) return null;
                                                        return wrapTeam(board, team, teamName);
                                                    }

                                                    if (sName.equals("getTeams")) {
                                                        java.util.Set<org.bukkit.scoreboard.Team> out = new java.util.HashSet<>();
                                                        for (net.minecraft.scoreboard.Team t : board.getTeams()) {
                                                            out.add(wrapTeam(board, t, t.getName()));
                                                        }
                                                        return out;
                                                    }

                                                    if (sName.equals("getEntries")) {
                                                        java.util.Set<String> entries = new java.util.HashSet<>();
                                                        for (net.minecraft.scoreboard.Team t : board.getTeams()) {
                                                            entries.addAll(t.getPlayerList());
                                                        }
                                                        return entries;
                                                    }

                                                    return getSafeDefault(sm.getReturnType());
                                                }
                                        );
                                    }
                                    return null;
                                }
                        );
                    }

                    if (methodName.equals("getItemFactory")) {
                        return (org.bukkit.inventory.ItemFactory) Proxy.newProxyInstance(
                                org.bukkit.inventory.ItemFactory.class.getClassLoader(),
                                new Class<?>[]{org.bukkit.inventory.ItemFactory.class},
                                (p, m, a) -> {
                                    if (m.getName().equals("getItemMeta")) {
                                        return (org.bukkit.inventory.meta.ItemMeta) Proxy.newProxyInstance(
                                                org.bukkit.inventory.meta.ItemMeta.class.getClassLoader(),
                                                new Class<?>[]{org.bukkit.inventory.meta.ItemMeta.class},
                                                (mp, mm, ma) -> getSafeDefault(mm.getReturnType())
                                        );
                                    }
                                    return getSafeDefault(m.getReturnType());
                                }
                        );
                    }

                    if (methodName.equals("createInventory")) {
                        String title = "Menu";
                        int size = 27;
                        if (args != null && args.length >= 2 && args[1] instanceof Integer) size = (Integer) args[1];
                        if (args != null && args.length >= 3 && args[2] instanceof String) title = (String) args[2];
                        return MythosInventory.createBukkitInventory(title, size);
                    }

                    if (methodName.equals("getServicesManager")) {
                        return (ServicesManager) Proxy.newProxyInstance(
                                ServicesManager.class.getClassLoader(),
                                new Class<?>[]{ServicesManager.class},
                                (p, m, a) -> {
                                    String mn = m.getName();
                                    if (mn.equals("register") && a != null && a.length >= 4) {
                                        @SuppressWarnings("unchecked")
                                        Class<Object> service = (Class<Object>) a[0];
                                        Object provider = a[1];
                                        Plugin owner = (Plugin) a[2];
                                        ServicePriority priority = (ServicePriority) a[3];
                                        registeredServices.put(service, provider);
                                        RegisteredServiceProvider<?> rsp = new RegisteredServiceProvider<>(
                                                service,
                                                provider,
                                                priority != null ? priority : ServicePriority.Normal,
                                                owner != null ? owner : DUMMY_PLUGIN
                                        );
                                        serviceRegistrations.put(service, rsp);
                                        return null;
                                    }
                                    if (mn.equals("getRegistration") && a != null && a.length >= 1) {
                                        @SuppressWarnings("unchecked")
                                        Class<Object> service = (Class<Object>) a[0];
                                        RegisteredServiceProvider<?> cached = serviceRegistrations.get(service);
                                        if (cached != null) return cached;
                                        Object provider = registeredServices.get(service);
                                        if (provider == null) return null;
                                        return new RegisteredServiceProvider<>(service, provider, ServicePriority.Normal, DUMMY_PLUGIN);
                                    }
                                    if (mn.equals("getRegistrations") && a != null && a.length >= 1) {
                                        if (a[0] instanceof Class<?> service) {
                                            RegisteredServiceProvider<?> rsp = serviceRegistrations.get(service);
                                            return rsp != null ? List.of(rsp) : List.of();
                                        }
                                        if (a[0] instanceof Plugin owner) {
                                            List<RegisteredServiceProvider<?>> list = new ArrayList<>();
                                            for (RegisteredServiceProvider<?> rsp : serviceRegistrations.values()) {
                                                if (rsp.getPlugin() == owner) list.add(rsp);
                                            }
                                            return list;
                                        }
                                    }
                                    if (mn.equals("load") && a != null && a.length >= 1) {
                                        return registeredServices.get((Class<?>) a[0]);
                                    }
                                    if (mn.equals("isProvidedFor") && a != null && a.length >= 1) {
                                        return registeredServices.containsKey((Class<?>) a[0]);
                                    }
                                    if (mn.equals("unregister") && a != null && a.length >= 1) {
                                        if (a.length == 2 && a[0] instanceof Class<?> svc) {
                                            registeredServices.remove(svc);
                                            serviceRegistrations.remove(svc);
                                        }
                                        return null;
                                    }
                                    if (mn.equals("unregisterAll") && a != null && a.length >= 1 && a[0] instanceof Plugin owner) {
                                        serviceRegistrations.entrySet().removeIf(e -> {
                                            if (e.getValue().getPlugin() == owner) {
                                                registeredServices.remove(e.getKey());
                                                return true;
                                            }
                                            return false;
                                        });
                                        return null;
                                    }
                                    if (mn.equals("getKnownServices")) {
                                        return new java.util.HashSet<>(registeredServices.keySet());
                                    }
                                    return getSafeDefault(m.getReturnType());
                                }
                        );
                    }

                    if (methodName.equals("getUnsafe")) {
                        return (UnsafeValues) Proxy.newProxyInstance(
                                UnsafeValues.class.getClassLoader(),
                                new Class<?>[]{UnsafeValues.class},
                                (p, m, a) -> {
                                    if (m.getName().equals("checkSupported")) return a[0];
                                    return getSafeDefault(m.getReturnType());
                                }
                        );
                    }

                    if (methodName.equals("getPluginManager")) {
                        return (PluginManager) Proxy.newProxyInstance(
                                PluginManager.class.getClassLoader(),
                                new Class<?>[]{PluginManager.class},
                                (p, m, a) -> {
                                    String mn = m.getName();
                                    if (mn.equals("registerEvents")) {
                                        eventManager.registerEvents((Listener) a[0], (Plugin) a[1]);
                                        return null;
                                    }
                                    if (mn.equals("callEvent")) {
                                        eventManager.callEvent((Event) a[0]);
                                        return null;
                                    }
                                    if (mn.equals("getPlugin") && a != null && a.length >= 1) {
                                        String wanted = String.valueOf(a[0]);
                                        if ("MythosCore".equalsIgnoreCase(wanted)) return DUMMY_PLUGIN;
                                        return pluginsByName.get(wanted.toLowerCase(java.util.Locale.ROOT));
                                    }
                                    if (mn.equals("getPlugins")) {
                                        return loadedPlugins.toArray(new Plugin[0]);
                                    }
                                    if (mn.equals("isPluginEnabled") && a != null && a.length >= 1) {
                                        if (a[0] instanceof String s) {
                                            if ("MythosCore".equalsIgnoreCase(s)) return true;
                                            Plugin found = pluginsByName.get(s.toLowerCase(java.util.Locale.ROOT));
                                            return found != null && found.isEnabled();
                                        }
                                        if (a[0] instanceof Plugin pl) {
                                            return pl.isEnabled();
                                        }
                                    }
                                    if (mn.equals("enablePlugin") && a != null && a[0] instanceof Plugin pl) {
                                        if (!pl.isEnabled()) {
                                            setPluginEnabledState(pl, true);
                                        }
                                        return null;
                                    }
                                    if (mn.equals("disablePlugin") && a != null && a[0] instanceof Plugin pl) {
                                        if (pl.isEnabled()) {
                                            setPluginEnabledState(pl, false);
                                        }
                                        return null;
                                    }
                                    if (mn.equals("addPermission") && a != null && a[0] instanceof org.bukkit.permissions.Permission perm) {
                                        permissions.put(perm.getName().toLowerCase(java.util.Locale.ROOT), perm);
                                        return null;
                                    }
                                    if (mn.equals("removePermission") && a != null) {
                                        if (a[0] instanceof org.bukkit.permissions.Permission perm) {
                                            permissions.remove(perm.getName().toLowerCase(java.util.Locale.ROOT));
                                        } else if (a[0] instanceof String name) {
                                            permissions.remove(name.toLowerCase(java.util.Locale.ROOT));
                                        }
                                        return null;
                                    }
                                    if (mn.equals("getPermission") && a != null && a[0] instanceof String name) {
                                        return permissions.get(name.toLowerCase(java.util.Locale.ROOT));
                                    }
                                    if (mn.equals("getPermissions")) {
                                        return new java.util.HashSet<>(permissions.values());
                                    }
                                    if (mn.equals("getDefaultPermissions")) {
                                        return new java.util.HashSet<>(permissions.values());
                                    }
                                    return getSafeDefault(m.getReturnType());
                                }
                        );
                    }

                    if (methodName.equals("getConsoleSender")) {
                        return (CommandSender) Proxy.newProxyInstance(
                                CommandSender.class.getClassLoader(),
                                new Class<?>[]{CommandSender.class},
                                (p, m, a) -> {
                                    if (m.getName().equals("sendMessage")) {
                                        if (a != null && a.length > 0 && a[0] instanceof String) System.out.println("[MythosConsole] " + a[0]);
                                        return null;
                                    }
                                    if (m.getName().equals("getName")) return "CONSOLE";
                                    if (m.getName().equals("isOp")) return true;
                                    if (m.getName().equals("hasPermission")) return true;
                                    return getSafeDefault(m.getReturnType());
                                }
                        );
                    }

                    if (methodName.equals("getOnlinePlayers")) {
                        List<Player> list = new ArrayList<>();
                        if (MythosCoreMod.MC_SERVER != null) {
                            for (net.minecraft.server.network.ServerPlayerEntity sp : MythosCoreMod.MC_SERVER.getPlayerManager().getPlayerList()) {
                                list.add(MythosPlayer.wrap(sp));
                            }
                        }
                        return list;
                    }

                    if ((methodName.equals("getPlayerExact") || methodName.equals("getPlayer")) && args != null && args.length >= 1) {
                        if (MythosCoreMod.MC_SERVER != null) {
                            if (args[0] instanceof java.util.UUID uid) {
                                net.minecraft.server.network.ServerPlayerEntity sp = MythosCoreMod.MC_SERVER.getPlayerManager().getPlayer(uid);
                                return sp != null ? MythosPlayer.wrap(sp) : null;
                            }
                            if (args[0] instanceof String wanted) {
                                for (net.minecraft.server.network.ServerPlayerEntity sp : MythosCoreMod.MC_SERVER.getPlayerManager().getPlayerList()) {
                                    if (sp.getName().getString().equalsIgnoreCase(wanted)) {
                                        return MythosPlayer.wrap(sp);
                                    }
                                }
                            }
                        }
                        return null;
                    }

                    if (methodName.equals("getOfflinePlayer") && args != null && args.length >= 1) {
                        java.util.UUID uid = args[0] instanceof java.util.UUID u
                                ? u
                                : java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + args[0]).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        String name = args[0] instanceof String s ? s : uid.toString().substring(0, 8);
                        if (MythosCoreMod.MC_SERVER != null) {
                            net.minecraft.server.network.ServerPlayerEntity online = MythosCoreMod.MC_SERVER.getPlayerManager().getPlayer(uid);
                            if (online != null) return MythosPlayer.wrap(online);
                        }
                        return (org.bukkit.OfflinePlayer) Proxy.newProxyInstance(
                                org.bukkit.OfflinePlayer.class.getClassLoader(),
                                new Class<?>[]{org.bukkit.OfflinePlayer.class},
                                (op, om, oa) -> {
                                    String omn = om.getName();
                                    if (omn.equals("getUniqueId")) return uid;
                                    if (omn.equals("getName")) return name;
                                    if (omn.equals("isOnline")) return false;
                                    if (omn.equals("getPlayer")) return null;
                                    if (omn.equals("hasPlayedBefore")) return true;
                                    return getSafeDefault(om.getReturnType());
                                }
                        );
                    }

                    if (methodName.equals("getOfflinePlayers")) {
                        return new org.bukkit.OfflinePlayer[0];
                    }

                    if (methodName.equals("getWorlds")) {
                        List<org.bukkit.World> worlds = new ArrayList<>();
                        if (MythosCoreMod.MC_SERVER != null) {
                            for (net.minecraft.server.world.ServerWorld sw : MythosCoreMod.MC_SERVER.getWorlds()) {
                                worlds.add(pl.mythoscore.entity.MythosWorld.wrap(sw));
                            }
                        }
                        return worlds;
                    }

                    if (methodName.equals("getWorld") && args != null && args.length >= 1 && MythosCoreMod.MC_SERVER != null) {
                        for (net.minecraft.server.world.ServerWorld sw : MythosCoreMod.MC_SERVER.getWorlds()) {
                            if (args[0] instanceof java.util.UUID uid) {
                                if (pl.mythoscore.entity.MythosWorld.toWorldUuid(sw).equals(uid)) {
                                    return pl.mythoscore.entity.MythosWorld.wrap(sw);
                                }
                            } else if (args[0] instanceof String wanted) {
                                String bName = pl.mythoscore.entity.MythosWorld.toBukkitName(sw);
                                String path = sw.getRegistryKey().getValue().getPath();
                                if (bName.equalsIgnoreCase(wanted) || path.equalsIgnoreCase(wanted)) {
                                    return pl.mythoscore.entity.MythosWorld.wrap(sw);
                                }
                            }
                        }
                        return null;
                    }

                    if (methodName.equals("getMessenger")) {
                        return (org.bukkit.plugin.messaging.Messenger) Proxy.newProxyInstance(
                                org.bukkit.plugin.messaging.Messenger.class.getClassLoader(),
                                new Class<?>[]{org.bukkit.plugin.messaging.Messenger.class},
                                (mp, mm, ma) -> {
                                    String mn = mm.getName();
                                    if (mn.equals("getOutgoingChannels") || mn.equals("getIncomingChannels")) {
                                        return java.util.Collections.emptySet();
                                    }
                                    return getSafeDefault(mm.getReturnType());
                                }
                        );
                    }

                    if (methodName.equals("broadcastMessage") && args != null && args.length >= 1) {
                        String msg = String.valueOf(args[0]);
                        int count = 0;
                        if (MythosCoreMod.MC_SERVER != null) {
                            for (net.minecraft.server.network.ServerPlayerEntity sp : MythosCoreMod.MC_SERVER.getPlayerManager().getPlayerList()) {
                                sp.sendMessage(net.minecraft.text.Text.literal(msg));
                                count++;
                            }
                        }
                        return count;
                    }

                    if (methodName.equals("createBossBar")) {
                        return MythosBossBar.create((String) args[0], (org.bukkit.boss.BarColor) args[1], (org.bukkit.boss.BarStyle) args[2]);
                    }

                    if (methodName.equals("getName")) return "MythosCore";
                    if (methodName.equals("getVersion")) return "1.0.0 (MC: 1.20.1)";
                    if (methodName.equals("getBukkitVersion")) return "1.20.1-R0.1-SNAPSHOT";
                    if (methodName.equals("getMaxPlayers")) return MythosCoreMod.MC_SERVER != null ? MythosCoreMod.MC_SERVER.getMaxPlayerCount() : 100;
                    if (methodName.equals("getOnlineMode")) return MythosCoreMod.MC_SERVER != null && MythosCoreMod.MC_SERVER.isOnlineMode();
                    if (methodName.equals("getPort")) return MythosCoreMod.MC_SERVER != null ? MythosCoreMod.MC_SERVER.getServerPort() : 25565;
                    if (methodName.equals("getIp")) return "";
                    if (methodName.equals("getMotd")) return MythosCoreMod.MC_SERVER != null ? MythosCoreMod.MC_SERVER.getServerMotd() : "MythosCore Server";
                    if (methodName.equals("getWorldContainer")) return new java.io.File(".");
                    if (methodName.equals("getUpdateFolder")) return "update";
                    if (methodName.equals("getUpdateFolderFile")) return new java.io.File("plugins/update");
                    if (methodName.equals("isPrimaryThread")) {
                        return MythosCoreMod.MC_SERVER == null || Thread.currentThread() == MythosCoreMod.MC_SERVER.getThread();
                    }

                    return getSafeDefault(method.getReturnType());
                }
        );

        Bukkit.setServer(serverInstance);
    }

    public static Object getSafeDefault(Class<?> returnType) {
        if (returnType == boolean.class || returnType == Boolean.class) return false;
        if (returnType == int.class || returnType == Integer.class) return 0;
        if (returnType == long.class || returnType == Long.class) return 0L;
        if (returnType == float.class || returnType == Float.class) return 0f;
        if (returnType == double.class || returnType == Double.class) return 0d;
        if (returnType == byte.class || returnType == Byte.class) return (byte) 0;
        if (returnType == short.class || returnType == Short.class) return (short) 0;
        if (returnType == String.class) return "";
        return null;
    }

    /**
     * Owija team Minecrafta w obiekt Team z Bukkita (proxy).
     * Wydzielone z lambdy, bo team jest potrzebny w kilku miejscach
     * (registerNewTeam, getTeam, getTeams) — bez tego byłaby kopia kodu.
     *
     * WAŻNE: po każdej zmianie (prefix/suffix/entry) wołamy board.updateScoreboardTeam(team),
     * inaczej klient nie dostanie pakietu i napis nad głową się nie zmieni.
     */
    private static org.bukkit.scoreboard.Team wrapTeam(net.minecraft.scoreboard.ServerScoreboard board,
                                                       net.minecraft.scoreboard.Team team,
                                                       String teamName) {
        return (org.bukkit.scoreboard.Team) Proxy.newProxyInstance(
                org.bukkit.scoreboard.Team.class.getClassLoader(),
                new Class<?>[]{org.bukkit.scoreboard.Team.class},
                (tp, tm, ta) -> {
                    String n = tm.getName();
                    if (n.equals("setPrefix")) {
                        team.setPrefix(net.minecraft.text.Text.literal((String) ta[0]));
                        board.updateScoreboardTeam(team);
                        return null;
                    }
                    if (n.equals("setSuffix")) {
                        team.setSuffix(net.minecraft.text.Text.literal((String) ta[0]));
                        board.updateScoreboardTeam(team);
                        return null;
                    }
                    if (n.equals("getPrefix")) {
                        return team.getPrefix().getString();
                    }
                    if (n.equals("getSuffix")) {
                        return team.getSuffix().getString();
                    }
                    if (n.equals("addEntry")) {
                        String entry = (String) ta[0];
                        if (!team.getPlayerList().contains(entry)) {
                            board.addPlayerToTeam(entry, team);
                            board.updateScoreboardTeam(team);
                        }
                        return null;
                    }
                    if (n.equals("removeEntry")) {
                        String entry = (String) ta[0];
                        if (team.getPlayerList().contains(entry)) {
                            board.removePlayerFromTeam(entry, team);
                            board.updateScoreboardTeam(team);
                        }
                        return null;
                    }
                    if (n.equals("hasEntry")) {
                        String entry = (String) ta[0];
                        return team.getPlayerList().contains(entry);
                    }
                    if (n.equals("getEntries")) {
                        return new java.util.HashSet<>(team.getPlayerList());
                    }
                    if (n.equals("unregister")) {
                        if (board.getTeam(team.getName()) != null) {
                            board.removeTeam(team);
                        }
                        return null;
                    }
                    if (n.equals("getName")) return teamName;
                    if (n.equals("toString")) return "MythosTeam{" + teamName + "}";
                    return getSafeDefault(tm.getReturnType());
                }
        );
    }
}