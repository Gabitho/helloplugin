package com.pvpheads.control;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.attribute.Attribute;

public class MobControl {
    private final Pig pig;

    public MobControl(World world, Location location) {
        this.pig = (Pig) world.spawnEntity(location, EntityType.PIG);

        Bukkit.getLogger().info("MobControl constructeur appelé");

        setup();
    }
    

    private void setup() {
        Bukkit.getLogger().info("Setup appelé");
        // Vie du mob
        pig.getAttribute(Attribute.MAX_HEALTH).setBaseValue(25.0);
        pig.setHealth(25.0);

        // Empêcher les comportements chiants
        pig.setSilent(true); // pas de sons
        pig.setCollidable(false); // évite collisions bizarres

        // Optionnel (à réfléchir plus tard)
        // pig.setAI(false); // ← pour debug seulement    
        Bukkit.getServer().dispatchCommand(
            Bukkit.getServer().getConsoleSender(),
            "say setup exécuté"
        );
    }

    public Location getLocation() {
        return pig.getLocation();
    }

    public Pig getEntity() {
        return pig;
    }
}