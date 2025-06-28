package com.firstplugin.listeners;

import com.firstplugin.Main;
import com.firstplugin.utils.XPUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.UUID;

public class tntshoot implements Listener {

    private final JavaPlugin plugin;
    private final HashMap<UUID, Long> chargingStartTimes = new HashMap<>();
    private final int COOLDOWN_TICKS = 40; // 2 secondes cooldown

    public tntshoot(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "tnt_launcher");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // Si cooldown en cours, ignorer
        if (player.getCooldown(Material.BOW) > 0) return;

        // On commence le chargement : on mémorise le moment
        chargingStartTimes.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onRelease(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "tnt_launcher");
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) return;

        // Vérifie si le joueur a commencé à charger
        if (!chargingStartTimes.containsKey(player.getUniqueId())) return;

        long startTime = chargingStartTimes.remove(player.getUniqueId());
        long duration = System.currentTimeMillis() - startTime; // en millisecondes
        double seconds = Math.min(duration / 1000.0, 3.0); // max 3 secondes de charge

        // Applique un cooldown visuel au joueur (animation du bouclier)
        player.setCooldown(Material.BOW, COOLDOWN_TICKS);

        // Vérifie XP
        if (!XPUtils.removeXP(player, 30)) {
            player.sendMessage("§cPas assez d'expérience !");
            return;
        }

        // Tire la TNT
        TNTPrimed tnt = player.getWorld().spawn(
                player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(1.2)),
                TNTPrimed.class
        );

        Vector direction = player.getLocation().getDirection().normalize().multiply(0.3 + seconds * 0.3);
        direction.setY(0.15 + seconds * 0.1);
        tnt.setVelocity(direction);
        tnt.setFuseTicks(40);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (tnt.isDead()) return;
                if (tnt.getVelocity().lengthSquared() < 0.01) {
                    tnt.setVelocity(new Vector(0, 0, 0));
                }
            }
        }.runTaskLater(plugin, 10);
    }
}
