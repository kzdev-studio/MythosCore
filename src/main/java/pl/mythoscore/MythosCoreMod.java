package pl.mythoscore;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.mythoscore.entity.MythosPlayer;
import pl.mythoscore.plugin.MythosPluginLoader;
import pl.mythoscore.server.*;

public class MythosCoreMod implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("mythoscore");
    public static MythosPermissionManager permissionManager;
    public static MythosEconomy economy;
    public static MythosClaimManager claimManager;
    public static net.minecraft.server.MinecraftServer MC_SERVER = null;

    @Override
    public void onInitialize() {
        MythosLauncher.printBanner();
        LOGGER.info("MythosCore został pomyślnie załadowany przez Fabric Loader!");

        MythosScheduler scheduler = new MythosScheduler();
        MythosEventManager eventManager = new MythosEventManager();
        permissionManager = new MythosPermissionManager();
        economy = new MythosEconomy();
        claimManager = new MythosClaimManager();

        MythosServer.init(scheduler, eventManager);

        try {
            net.milkbowl.vault.economy.Economy vaultEconomy = new MythosVaultEconomy(economy);
            Bukkit.getServicesManager().register(net.milkbowl.vault.economy.Economy.class, vaultEconomy, MythosServer.DUMMY_PLUGIN, org.bukkit.plugin.ServicePriority.Normal);
            net.milkbowl.vault.permission.Permission vaultPerms = new MythosVaultPermissions(permissionManager);
            Bukkit.getServicesManager().register(net.milkbowl.vault.permission.Permission.class, vaultPerms, MythosServer.DUMMY_PLUGIN, org.bukkit.plugin.ServicePriority.Normal);
            LOGGER.info("Vault Economy i Permissions zarejestrowane pomyślnie!");
        } catch (Exception e) {
            LOGGER.error("Nie udało się zarejestrować Vaulta", e);
        }

        ServerTickEvents.END_SERVER_TICK.register(minecraftServer -> {
            MC_SERVER = minecraftServer;
            scheduler.tick();
            MythosNametagManager.tick(minecraftServer);
        });

        MythosPluginLoader pluginLoader = new MythosPluginLoader();
        pluginLoader.loadPlugins();

        MythosChatListener.register();
        MythosBlockListener.register();
        MythosDamageListener.register();

        // --- OBSŁUGA WEJŚCIA GRACZA ---
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity fabricPlayer = handler.getPlayer();
            Player bukkitPlayer = MythosPlayer.wrap(fabricPlayer);
            LOGGER.info("Gracz {} wszedł na serwer.", bukkitPlayer.getName());

            // Rangi zarządza AethelRanks
            if (economy.getBalance(bukkitPlayer.getName()) == 0.0) {
                economy.setBalance(bukkitPlayer.getName(), 100.0);
                bukkitPlayer.sendMessage("§aOtrzymałeś 100 startowych monet na start!");
            }

            PlayerJoinEvent joinEvent = new PlayerJoinEvent(bukkitPlayer, bukkitPlayer.getName() + " dołączył do gry!");
            Bukkit.getPluginManager().callEvent(joinEvent);
        });

        // --- NOWE: OBSŁUGA WYJŚCIA GRACZA ---
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity fabricPlayer = handler.getPlayer();
            MythosNametagManager.removeNametag(fabricPlayer.getUuid());
            Player bukkitPlayer = MythosPlayer.wrap(fabricPlayer);
            LOGGER.info("Gracz {} wyszedł z serwera.", bukkitPlayer.getName());

            PlayerQuitEvent quitEvent = new PlayerQuitEvent(bukkitPlayer, bukkitPlayer.getName() + " opuścił grę!");
            Bukkit.getPluginManager().callEvent(quitEvent);
        });
        // -------------------------------------

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.server.command.CommandManager.literal("claim").executes(context -> {
                var player = context.getSource().getPlayer();
                if (player != null && claimManager.claim(player.getWorld().getRegistryKey().getValue().getPath(), (int)player.getX(), (int)player.getZ(), player.getName().getString())) {
                    player.sendMessage(net.minecraft.text.Text.literal("§aPomyślnie zająłeś ten teren!"), false);
                } else if (player != null) {
                    player.sendMessage(net.minecraft.text.Text.literal("§cTen teren jest już zajęty!"), false);
                }
                return 1;
            }));

            dispatcher.register(net.minecraft.server.command.CommandManager.literal("unclaim").executes(context -> {
                var player = context.getSource().getPlayer();
                if (player != null && claimManager.unclaim(player.getWorld().getRegistryKey().getValue().getPath(), (int)player.getX(), (int)player.getZ(), player.getName().getString())) {
                    player.sendMessage(net.minecraft.text.Text.literal("§aPomyślnie zwolniłeś ten teren!"), false);
                } else if (player != null) {
                    player.sendMessage(net.minecraft.text.Text.literal("§cNie jesteś właścicielem tego terenu!"), false);
                }
                return 1;
            }));

            for (String cmdName : MythosServer.getCommandMap().getKnownCommands().keySet()) {
                try {
                    dispatcher.register(
                            net.minecraft.server.command.CommandManager.literal(cmdName)
                                    .executes(context -> {
                                        var player = context.getSource().getPlayer();
                                        org.bukkit.command.CommandSender sender = (player != null)
                                                ? MythosPlayer.wrap(player)
                                                : Bukkit.getConsoleSender();
                                        return MythosServer.getCommandMap().dispatch(sender, cmdName) ? 1 : 0;
                                    })
                                    .then(net.minecraft.server.command.CommandManager.argument("args", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                            .suggests((context, builder) -> {
                                                var player = context.getSource().getPlayer();
                                                org.bukkit.command.CommandSender sender = (player != null)
                                                        ? MythosPlayer.wrap(player)
                                                        : Bukkit.getConsoleSender();
                                                String input = context.getInput();
                                                String raw = input.startsWith("/") ? input.substring(1) : input;
                                                java.util.List<String> suggestions = MythosServer.getCommandMap().tabComplete(sender, raw);
                                                if (suggestions != null) {
                                                    String remaining = builder.getRemaining().toLowerCase();
                                                    for (String s : suggestions) {
                                                        if (s != null && s.toLowerCase().startsWith(remaining)) {
                                                            builder.suggest(s);
                                                        }
                                                    }
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(context -> {
                                                var player = context.getSource().getPlayer();
                                                org.bukkit.command.CommandSender sender = (player != null)
                                                        ? MythosPlayer.wrap(player)
                                                        : Bukkit.getConsoleSender();
                                                String args = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "args");
                                                return MythosServer.getCommandMap().dispatch(sender, cmdName + " " + args) ? 1 : 0;
                                            })
                                    )
                    );
                } catch (Exception e) {
                    LOGGER.error("Błąd rejestracji komendy w Fabric: " + cmdName, e);
                }
            }
        });

        LOGGER.info("MythosCore w pełni zainicjalizowany. Oczekiwanie na graczy...");
    }
}