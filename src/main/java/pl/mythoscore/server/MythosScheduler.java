package pl.mythoscore.server;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import pl.mythoscore.MythosCoreMod;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class MythosScheduler {

    private final List<TaskEntry> pendingTasks = new CopyOnWriteArrayList<>();
    private final AtomicInteger nextTaskId = new AtomicInteger(1);
    private final ExecutorService asyncPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "MythosCore-AsyncScheduler");
        t.setDaemon(true);
        return t;
    });

    public BukkitScheduler createScheduler() {
        return (BukkitScheduler) Proxy.newProxyInstance(
                BukkitScheduler.class.getClassLoader(),
                new Class<?>[]{BukkitScheduler.class},
                (proxy, method, args) -> {
                    String name = method.getName();

                    if (name.equals("runTask") && args != null && args.length >= 2) {
                        return schedule((Plugin) args[0], args[1], 0L, -1L, true);
                    }
                    if (name.equals("runTaskLater") && args != null && args.length >= 3) {
                        return schedule((Plugin) args[0], args[1], ((Number) args[2]).longValue(), -1L, true);
                    }
                    if (name.equals("runTaskTimer") && args != null && args.length >= 4) {
                        return schedule((Plugin) args[0], args[1], ((Number) args[2]).longValue(), ((Number) args[3]).longValue(), true);
                    }
                    if (name.equals("runTaskAsynchronously") && args != null && args.length >= 2) {
                        return schedule((Plugin) args[0], args[1], 0L, -1L, false);
                    }
                    if (name.equals("runTaskLaterAsynchronously") && args != null && args.length >= 3) {
                        return schedule((Plugin) args[0], args[1], ((Number) args[2]).longValue(), -1L, false);
                    }
                    if (name.equals("runTaskTimerAsynchronously") && args != null && args.length >= 4) {
                        return schedule((Plugin) args[0], args[1], ((Number) args[2]).longValue(), ((Number) args[3]).longValue(), false);
                    }
                    if (name.equals("scheduleSyncDelayedTask") && args != null && args.length >= 2) {
                        long delay = args.length >= 3 ? ((Number) args[2]).longValue() : 0L;
                        return schedule((Plugin) args[0], args[1], delay, -1L, true).getTaskId();
                    }
                    if (name.equals("scheduleSyncRepeatingTask") && args != null && args.length >= 4) {
                        return schedule((Plugin) args[0], args[1], ((Number) args[2]).longValue(), ((Number) args[3]).longValue(), true).getTaskId();
                    }
                    if (name.equals("scheduleAsyncDelayedTask") && args != null && args.length >= 2) {
                        long delay = args.length >= 3 ? ((Number) args[2]).longValue() : 0L;
                        return schedule((Plugin) args[0], args[1], delay, -1L, false).getTaskId();
                    }
                    if (name.equals("scheduleAsyncRepeatingTask") && args != null && args.length >= 4) {
                        return schedule((Plugin) args[0], args[1], ((Number) args[2]).longValue(), ((Number) args[3]).longValue(), false).getTaskId();
                    }
                    if (name.equals("cancelTask") && args != null && args.length >= 1) {
                        int id = (Integer) args[0];
                        for (TaskEntry e : pendingTasks) {
                            if (e.id == id) e.cancelled = true;
                        }
                        return null;
                    }
                    if (name.equals("cancelTasks") && args != null && args.length >= 1) {
                        Plugin owner = (Plugin) args[0];
                        for (TaskEntry e : pendingTasks) {
                            if (e.owner == owner) e.cancelled = true;
                        }
                        return null;
                    }
                    if (name.equals("isCurrentlyRunning")) return false;
                    if (name.equals("isQueued") && args != null && args.length >= 1) {
                        int id = (Integer) args[0];
                        for (TaskEntry e : pendingTasks) {
                            if (e.id == id && !e.cancelled) return true;
                        }
                        return false;
                    }
                    if (name.equals("getPendingTasks")) {
                        List<BukkitTask> list = new ArrayList<>();
                        for (TaskEntry e : pendingTasks) {
                            if (!e.cancelled) list.add(e.bukkitTask);
                        }
                        return list;
                    }
                    return MythosServer.getSafeDefault(method.getReturnType());
                }
        );
    }

    @SuppressWarnings("unchecked")
    private BukkitTask schedule(Plugin owner, Object taskObj, long delay, long period, boolean sync) {
        int id = nextTaskId.getAndIncrement();
        TaskEntry entry = new TaskEntry(id, owner, Math.max(0L, delay), period, sync);
        BukkitTask bukkitTask = (BukkitTask) Proxy.newProxyInstance(
                BukkitTask.class.getClassLoader(),
                new Class<?>[]{BukkitTask.class},
                (p, m, a) -> {
                    String mn = m.getName();
                    if (mn.equals("getTaskId")) return entry.id;
                    if (mn.equals("getOwner")) return entry.owner;
                    if (mn.equals("isSync")) return entry.sync;
                    if (mn.equals("isCancelled")) return entry.cancelled;
                    if (mn.equals("cancel")) {
                        entry.cancelled = true;
                        return null;
                    }
                    return MythosServer.getSafeDefault(m.getReturnType());
                }
        );
        entry.bukkitTask = bukkitTask;

        if (taskObj instanceof Runnable r) {
            entry.task = r;
        } else if (taskObj instanceof Consumer<?> c) {
            Consumer<BukkitTask> consumer = (Consumer<BukkitTask>) c;
            entry.task = () -> consumer.accept(bukkitTask);
        } else {
            entry.task = () -> {};
        }

        pendingTasks.add(entry);
        return bukkitTask;
    }

    public void tick() {
        List<TaskEntry> toRemove = new ArrayList<>();

        for (TaskEntry entry : pendingTasks) {
            if (entry.cancelled || (entry.owner != null && !entry.owner.isEnabled())) {
                toRemove.add(entry);
                continue;
            }
            if (entry.delay > 0) {
                entry.delay--;
                continue;
            }

            if (entry.sync) {
                try {
                    entry.task.run();
                } catch (Throwable t) {
                    MythosCoreMod.LOGGER.error("Błąd w zadaniu sync (taskId={}):", entry.id, t);
                }
            } else {
                asyncPool.submit(() -> {
                    if (!entry.cancelled) {
                        try {
                            entry.task.run();
                        } catch (Throwable t) {
                            MythosCoreMod.LOGGER.error("Błąd w zadaniu async (taskId={}):", entry.id, t);
                        }
                    }
                });
            }

            if (entry.period >= 0 && !entry.cancelled) {
                entry.delay = Math.max(1L, entry.period);
            } else {
                toRemove.add(entry);
            }
        }

        if (!toRemove.isEmpty()) {
            pendingTasks.removeAll(toRemove);
        }
    }

    private static class TaskEntry {
        final int id;
        final Plugin owner;
        final long period;
        final boolean sync;
        Runnable task;
        BukkitTask bukkitTask;
        long delay;
        volatile boolean cancelled;

        TaskEntry(int id, Plugin owner, long delay, long period, boolean sync) {
            this.id = id;
            this.owner = owner;
            this.delay = delay;
            this.period = period;
            this.sync = sync;
        }
    }
}
