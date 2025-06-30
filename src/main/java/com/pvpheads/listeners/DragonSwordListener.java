package com.pvpheads.listeners;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class DragonSwordListener implements Listener {

    private final NamespacedKey key = new NamespacedKey("pvpheads", "dragon_sword");

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        // Ignore les clics main gauche (évite double déclenchement)
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;


        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta()) return;

        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // 🔥 Lancer un DragonFireball (souffle violet)
        DragonFireball fireball = player.launchProjectile(DragonFireball.class);
        fireball.setDirection(player.getLocation().getDirection()); // vers le regard du joueur
        fireball.setYield(0); // pas d'explosion
        fireball.setIsIncendiary(false);

        // 💬 Effet sonore : grognement de dragon
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1.0f, 1.0f);
    }
}
