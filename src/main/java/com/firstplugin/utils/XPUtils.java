package com.firstplugin.utils;

import org.bukkit.entity.Player;

public class XPUtils {

    public static int getTotalXP(Player player) {
        int level = player.getLevel();
        float progress = player.getExp(); // de 0.0 à 1.0
        int xp = getTotalXPAtLevel(level);
        int next = getXpToNextLevel(level);

        return xp + Math.round(progress * next);
    }

    public static boolean removeXP(Player player, int amount) {
        int total = getTotalXP(player);
        if (total < amount) return false;

        int newTotal = total - amount;
        setTotalXP(player, newTotal);
        return true;
    }

    private static int getTotalXPAtLevel(int level) {
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int)(2.5 * level * level - 40.5 * level + 360);
        return (int)(4.5 * level * level - 162.5 * level + 2220);
    }

    private static int getXpToNextLevel(int level) {
        if (level <= 15) return 2 * level + 7;
        if (level <= 30) return 5 * level - 38;
        return 9 * level - 158;
    }

    public static void setTotalXP(Player player, int amount) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);

        int level = 0;
        while (getTotalXPAtLevel(level) <= amount) {
            level++;
        }
        level--;

        player.setLevel(level);
        int xpAtLevel = getTotalXPAtLevel(level);
        int xpIntoLevel = amount - xpAtLevel;
        float progress = (float) xpIntoLevel / (float) getXpToNextLevel(level);
        player.setExp(progress);
    }
}
