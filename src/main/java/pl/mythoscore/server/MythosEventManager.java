package pl.mythoscore.server;

import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MythosEventManager {

    // Lista par: Listener + Plugin, do którego należy
    private final List<RegisteredListener> listeners = new ArrayList<>();

    public void registerEvents(Listener listener, Plugin plugin) {
        listeners.add(new RegisteredListener(listener, plugin));
    }

    public void callEvent(Event event) {
        for (RegisteredListener reg : listeners) {
            for (Method method : reg.listener.getClass().getMethods()) {
                EventHandler handler = method.getAnnotation(EventHandler.class);
                if (handler != null && method.getParameterCount() == 1) {
                    // Sprawdzamy, czy metoda przyjmuje nasz event (lub jego klasę nadrzędną)
                    if (method.getParameterTypes()[0].isAssignableFrom(event.getClass())) {
                        try {
                            method.invoke(reg.listener, event);
                        } catch (Exception e) {
                            System.err.println("Błąd podczas wywoływania eventu w pluginie " + reg.plugin.getName());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private static class RegisteredListener {
        Listener listener;
        Plugin plugin;
        RegisteredListener(Listener listener, Plugin plugin) {
            this.listener = listener;
            this.plugin = plugin;
        }
    }
}