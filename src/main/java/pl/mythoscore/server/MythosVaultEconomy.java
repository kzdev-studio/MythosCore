package pl.mythoscore.server;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.util.List;

public class MythosVaultEconomy implements Economy {

    private final MythosEconomy economy;

    public MythosVaultEconomy(MythosEconomy economy) {
        this.economy = economy;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "MythosCoreEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return amount + " monet";
    }

    @Override
    public String currencyNamePlural() {
        return "monet";
    }

    @Override
    public String currencyNameSingular() {
        return "moneta";
    }

    // --- NOWE METODY: Tworzenie konta (Vault 1.7) ---
    @Override
    public boolean createPlayerAccount(String playerName) { return true; }
    @Override
    public boolean createPlayerAccount(OfflinePlayer player) { return true; }
    @Override
    public boolean createPlayerAccount(String playerName, String worldName) { return true; }
    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) { return true; }
    // -----------------------------------------------

    @Override
    public boolean hasAccount(String playerName) { return true; }
    @Override
    public boolean hasAccount(OfflinePlayer player) { return true; }
    @Override
    public boolean hasAccount(String playerName, String worldName) { return true; }
    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) { return true; }

    @Override
    public double getBalance(String playerName) { return economy.getBalance(playerName); }
    @Override
    public double getBalance(OfflinePlayer player) { return economy.getBalance(player.getName()); }
    @Override
    public double getBalance(String playerName, String world) { return getBalance(playerName); }
    @Override
    public double getBalance(OfflinePlayer player, String world) { return getBalance(player); }

    @Override
    public boolean has(String playerName, double amount) { return getBalance(playerName) >= amount; }
    @Override
    public boolean has(OfflinePlayer player, double amount) { return getBalance(player) >= amount; }
    @Override
    public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) { return has(player, amount); }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        if (economy.removeBalance(playerName, amount)) {
            return new EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null);
        } else {
            return new EconomyResponse(0, getBalance(playerName), EconomyResponse.ResponseType.FAILURE, "Brak wystarczających środków");
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) { return withdrawPlayer(player.getName(), amount); }
    @Override
    public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) { return withdrawPlayer(player, amount); }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        economy.addBalance(playerName, amount);
        return new EconomyResponse(amount, getBalance(playerName), EconomyResponse.ResponseType.SUCCESS, null);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) { return depositPlayer(player.getName(), amount); }
    @Override
    public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) { return depositPlayer(player, amount); }

    // --- METODY BANKOWE (Zaślepki) ---
    @Override public EconomyResponse createBank(String name, String player) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banki nie są wspierane"); }
    @Override public EconomyResponse createBank(String name, OfflinePlayer player) { return createBank(name, player.getName()); }
    @Override public EconomyResponse deleteBank(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banki nie są wspierane"); }
    @Override public EconomyResponse bankBalance(String name) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banki nie są wspierane"); }
    @Override public EconomyResponse bankHas(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banki nie są wspierane"); }
    @Override public EconomyResponse bankWithdraw(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banki nie są wspierane"); }
    @Override public EconomyResponse bankDeposit(String name, double amount) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banki nie są wspierane"); }
    @Override public EconomyResponse isBankOwner(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banki nie są wspierane"); }
    @Override public EconomyResponse isBankOwner(String name, OfflinePlayer player) { return isBankOwner(name, player.getName()); }
    @Override public EconomyResponse isBankMember(String name, String playerName) { return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banki nie są wspierane"); }
    @Override public EconomyResponse isBankMember(String name, OfflinePlayer player) { return isBankMember(name, player.getName()); }
    @Override public List<String> getBanks() { return List.of(); }
}