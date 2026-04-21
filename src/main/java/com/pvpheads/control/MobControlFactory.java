package com.pvpheads.control;

import org.bukkit.Location;
import org.bukkit.World;

public class MobControlFactory {

    public static MobControl createPig(World world, Location location) {
        return new MobControl(world, location);
    }
}