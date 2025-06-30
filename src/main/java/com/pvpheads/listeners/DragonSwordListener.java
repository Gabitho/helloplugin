// Le listener s'enregistre pour les événements
package com.pvpheads.listeners;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class DragonSwordListener implements Listener {

    // Le tag utilisé pour reconnaître l'épée du dragon
    private final NamespacedKey key = new NamespacedKey("pvpheads", "dragon_sword");

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        // Ignore les clics avec la main gauche pour éviter les doublons
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();

        // On récupère l'item dans la main principale
        ItemStack item = player.getInventory().getItemInMainHand();

        // Vérifie que c’est un item avec des métadonnées
        if (item == null || !item.hasItemMeta()) return;

        // Vérifie que l’item a bien le tag dragon_sword
        if (!item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // On lance une boule de feu depuis le joueur (direction du regard)
        Fireball fireball = player.launchProjectile(Fireball.class);
        fireball.setDirection(player.getLocation().getDirection()); // direction = regard du joueur
        fireball.setYield(2); // puissance de l'explosion
        fireball.setIsIncendiary(false); // n’allume pas le feu
        fireball.setCustomName("DragonBreath"); // nom interne
        fireball.setCustomNameVisible(false);

        // Joue un son de dragon au lancement
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.5f);
    }
}
