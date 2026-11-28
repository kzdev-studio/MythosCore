package pl.mythoscore.server;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MythosCommandMap implements CommandMap {

    private final Map<String, Command> knownCommands = new HashMap<>();

    @Override
    public void registerAll(String fallbackPrefix, List<Command> commands) {
        for (Command command : commands) {
            register(fallbackPrefix, command);
        }
    }

    @Override
    public boolean register(String fallbackPrefix, Command command) {
        return register(command.getName(), fallbackPrefix, command);
    }

    @Override
    public boolean register(String label, String fallbackPrefix, Command command) {
        knownCommands.put(label.toLowerCase(), command);
        knownCommands.put((fallbackPrefix + ":" + label).toLowerCase(), command);
        if (command.getAliases() != null) {
            for (String alias : command.getAliases()) {
                if (alias != null && !alias.isBlank()) {
                    knownCommands.putIfAbsent(alias.toLowerCase(), command);
                    knownCommands.putIfAbsent((fallbackPrefix + ":" + alias).toLowerCase(), command);
                }
            }
        }
        return true;
    }

    @Override
    public boolean dispatch(CommandSender sender, String commandLine) throws CommandException {
        String[] split = commandLine.split(" ");
        String commandName = split[0];

        Command command = knownCommands.get(commandName.toLowerCase());
        if (command == null) {
            return false;
        }

        String[] args = new String[split.length - 1];
        System.arraycopy(split, 1, args, 0, args.length);

        return command.execute(sender, commandName, args);
    }

    @Override
    public void clearCommands() {
        knownCommands.clear();
    }

    @Override
    public Command getCommand(String name) {
        return knownCommands.get(name.toLowerCase());
    }

    // Pierwsza wersja tabComplete
    @Override
    public List<String> tabComplete(CommandSender sender, String cmdLine) throws IllegalArgumentException {
        return tabComplete(sender, cmdLine, null);
    }

    // Druga wersja tabComplete (z lokalizacją)
    @Override
    public List<String> tabComplete(CommandSender sender, String cmdLine, Location location) throws IllegalArgumentException {
        if (cmdLine == null || sender == null) {
            return java.util.Collections.emptyList();
        }
        String line = cmdLine.startsWith("/") ? cmdLine.substring(1) : cmdLine;
        int spaceIndex = line.indexOf(' ');
        if (spaceIndex == -1) {
            String prefix = line.toLowerCase();
            java.util.List<String> matches = new java.util.ArrayList<>();
            for (String key : knownCommands.keySet()) {
                if (key.startsWith(prefix)) {
                    matches.add(key);
                }
            }
            return matches;
        }
        String cmdName = line.substring(0, spaceIndex).toLowerCase();
        Command command = knownCommands.get(cmdName);
        if (command == null) {
            return java.util.Collections.emptyList();
        }
        String argLine = line.substring(spaceIndex + 1);
        String[] args = argLine.split(" ", -1);
        return command.tabComplete(sender, cmdName, args);
    }

    // Metoda pomocnicza dla naszego silnika.
    // UWAGA: Nie ma tu @Override, bo nie ma jej w czystym API Spigota 1.20.1!
    public Map<String, Command> getKnownCommands() {
        return knownCommands;
    }
}