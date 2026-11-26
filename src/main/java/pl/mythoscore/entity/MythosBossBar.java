package pl.mythoscore.entity;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar; // POPRAWNY IMPORT
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;

import java.lang.reflect.Proxy;

public class MythosBossBar {

    public static org.bukkit.boss.BossBar create(String title, BarColor color, BarStyle style) {
        // 1. Tłumaczymy kolory Bukkita na kolory Fabrica
        BossBar.Color fabricColor = BossBar.Color.RED;
        if (color == BarColor.BLUE) fabricColor = BossBar.Color.BLUE;
        else if (color == BarColor.GREEN) fabricColor = BossBar.Color.GREEN;
        else if (color == BarColor.PINK) fabricColor = BossBar.Color.PINK;
        else if (color == BarColor.PURPLE) fabricColor = BossBar.Color.PURPLE;
        else if (color == BarColor.WHITE) fabricColor = BossBar.Color.WHITE;
        else if (color == BarColor.YELLOW) fabricColor = BossBar.Color.YELLOW;

        // 2. Tłumaczymy styl paska
        BossBar.Style fabricStyle = BossBar.Style.PROGRESS;
        if (style == BarStyle.SEGMENTED_6) fabricStyle = BossBar.Style.NOTCHED_6;
        else if (style == BarStyle.SEGMENTED_10) fabricStyle = BossBar.Style.NOTCHED_10;
        else if (style == BarStyle.SEGMENTED_12) fabricStyle = BossBar.Style.NOTCHED_12;
        else if (style == BarStyle.SEGMENTED_20) fabricStyle = BossBar.Style.NOTCHED_20;

        // 3. Tworzymy pasek w Fabric
        ServerBossBar fabricBar = new ServerBossBar(Text.literal(title), fabricColor, fabricStyle);
        fabricBar.setPercent(1.0f);

        // 4. Zwracamy Proxy dla Bukkita
        return (org.bukkit.boss.BossBar) Proxy.newProxyInstance(
                org.bukkit.boss.BossBar.class.getClassLoader(),
                new Class<?>[]{org.bukkit.boss.BossBar.class},
                (proxy, method, args) -> {
                    String methodName = method.getName();

                    if (methodName.equals("setTitle")) {
                        fabricBar.setName(Text.literal((String) args[0]));
                        return null;
                    }
                    if (methodName.equals("getTitle")) return fabricBar.getName().getString();
                    if (methodName.equals("setProgress")) {
                        fabricBar.setPercent((float) args[0]);
                        return null;
                    }
                    if (methodName.equals("getProgress")) return fabricBar.getPercent();

                    if (methodName.equals("addPlayer")) {
                        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) args[0];
                        ServerPlayerEntity fabricPlayer = MythosPlayer.getFabricPlayer(bukkitPlayer);
                        if (fabricPlayer != null) {
                            fabricBar.addPlayer(fabricPlayer);
                        }
                        return null;
                    }
                    if (methodName.equals("removePlayer")) {
                        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) args[0];
                        ServerPlayerEntity fabricPlayer = MythosPlayer.getFabricPlayer(bukkitPlayer);
                        if (fabricPlayer != null) {
                            fabricBar.removePlayer(fabricPlayer);
                        }
                        return null;
                    }
                    if (methodName.equals("removeAll")) {
                        fabricBar.clearPlayers();
                        return null;
                    }

                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    if (returnType == double.class) return 0d;
                    if (returnType == float.class) return 0f;
                    return null;
                }
        );
    }
}