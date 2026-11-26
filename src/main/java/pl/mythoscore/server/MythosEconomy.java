package pl.mythoscore.server;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MythosEconomy {

    private final MythosConfig config;
    private final Map<String, Double> balances = new HashMap<>();

    public MythosEconomy() {
        // Zapisujemy dane w run/plugins/mythoscore/economy.yml
        this.config = new MythosConfig(new File("plugins/mythoscore/economy.yml"));
        loadEconomy();
    }

    @SuppressWarnings("unchecked")
    private void loadEconomy() {
        Object ecoObj = config.get("balances");
        if (ecoObj instanceof Map) {
            balances.putAll((Map<String, Double>) ecoObj);
        }
    }

    public void saveEconomy() {
        config.set("balances", balances);
    }

    public double getBalance(String playerName) {
        return balances.getOrDefault(playerName.toLowerCase(), 0.0);
    }

    public void setBalance(String playerName, double amount) {
        balances.put(playerName.toLowerCase(), amount);
        saveEconomy();
    }

    public void addBalance(String playerName, double amount) {
        setBalance(playerName, getBalance(playerName) + amount);
    }

    public boolean removeBalance(String playerName, double amount) {
        double current = getBalance(playerName);
        if (current >= amount) {
            setBalance(playerName, current - amount);
            return true; // Udalo sie pobrac kase
        }
        return false; // Brak wystarczajacej ilosci monet
    }
}