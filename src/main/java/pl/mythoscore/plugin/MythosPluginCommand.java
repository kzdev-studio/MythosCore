package pl.mythoscore.plugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class MythosPluginCommand extends Command {

    private final Plugin owner;

    protected MythosPluginCommand(String name, Plugin owner) {
        super(name);
        this.owner = owner;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        // Przekazujemy wykonanie komendy do nasłuchiwacza w pluginie
        return owner.onCommand(sender, this, commandLabel, args);
    }
}