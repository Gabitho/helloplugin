package com.pvpheads.displaymob;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager to spawn and track DisplayMobs.
 */
public class DisplayMobManager {
    private static final Map<UUID, DisplayMob> MOB_MAP = new ConcurrentHashMap<>();
    private final Plugin plugin;

    public DisplayMobManager(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawn a new DisplayMob and register it.
     * Example: spawn(loc, 1.0f, 3) -> scale 1.0, slime size 3
     */
    public DisplayMob spawn(Location loc, float visualScale, int hitboxSize) {
        DisplayMob mob = new DisplayMob(plugin, 20); // default 20 HP; change as needed
        mob.spawn(loc, visualScale, hitboxSize);
        MOB_MAP.put(mob.getId(), mob);
        return mob;
    }

    public DisplayMob get(UUID id) {
        return MOB_MAP.get(id);
    }

    public void remove(UUID id) {
        DisplayMob mob = MOB_MAP.remove(id);
        if (mob != null) mob.remove();
    }

    public void removeAll() {
        for (UUID id : MOB_MAP.keySet()) {
            DisplayMob mob = MOB_MAP.remove(id);
            if (mob != null) mob.remove();
        }
    }
}
