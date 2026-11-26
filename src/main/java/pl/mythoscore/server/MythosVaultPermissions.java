package pl.mythoscore.server;

import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;

public class MythosVaultPermissions extends Permission {

    private final MythosPermissionManager permissionManager;

    public MythosVaultPermissions(MythosPermissionManager manager) {
        this.permissionManager = manager;
    }

    @Override
    public String getName() {
        return "MythosCorePermissions";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean hasSuperPermsCompat() {
        return true;
    }

    @Override
    public boolean playerHas(String world, String player, String permission) {
        return permissionManager.hasPermission(player, permission);
    }

    @Override
    public boolean playerHas(String world, OfflinePlayer player, String permission) {
        return playerHas(world, player.getName(), permission);
    }

    @Override
    public boolean playerAdd(String world, String player, String permission) {
        return false;
    }

    @Override
    public boolean playerAdd(String world, OfflinePlayer player, String permission) {
        return false;
    }

    @Override
    public boolean playerRemove(String world, String player, String permission) {
        return false;
    }

    @Override
    public boolean playerRemove(String world, OfflinePlayer player, String permission) {
        return false;
    }

    @Override
    public boolean groupHas(String world, String group, String permission) {
        if (group.equalsIgnoreCase("VIP")) return true;
        return permission.equalsIgnoreCase("aethelgard.basic");
    }

    @Override
    public boolean groupAdd(String world, String group, String permission) {
        return false;
    }

    @Override
    public boolean groupRemove(String world, String group, String permission) {
        return false;
    }

    @Override
    public boolean playerInGroup(String world, String player, String group) {
        return permissionManager.getRank(player).equalsIgnoreCase(group);
    }

    @Override
    public boolean playerInGroup(String world, OfflinePlayer player, String group) {
        return playerInGroup(world, player.getName(), group);
    }

    @Override
    public boolean playerAddGroup(String world, String player, String group) {
        permissionManager.setRank(player, group);
        return true;
    }

    @Override
    public boolean playerAddGroup(String world, OfflinePlayer player, String group) {
        return playerAddGroup(world, player.getName(), group);
    }

    @Override
    public boolean playerRemoveGroup(String world, String player, String group) {
        permissionManager.setRank(player, "Gracz");
        return true;
    }

    @Override
    public boolean playerRemoveGroup(String world, OfflinePlayer player, String group) {
        return playerRemoveGroup(world, player.getName(), group);
    }

    @Override
    public String[] getPlayerGroups(String world, String player) {
        return new String[]{permissionManager.getRank(player)};
    }

    @Override
    public String[] getPlayerGroups(String world, OfflinePlayer player) {
        return getPlayerGroups(world, player.getName());
    }

    @Override
    public String getPrimaryGroup(String world, String player) {
        return permissionManager.getRank(player);
    }

    @Override
    public String getPrimaryGroup(String world, OfflinePlayer player) {
        return getPrimaryGroup(world, player.getName());
    }

    @Override
    public String[] getGroups() {
        return new String[]{"Gracz", "VIP"};
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }
}