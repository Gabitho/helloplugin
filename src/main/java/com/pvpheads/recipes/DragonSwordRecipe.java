package com.pvpheads.recipes;

import com.pvpheads.Main;

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;

import org.bukkit.persistence.PersistentDataType;

/**
 * DragonSwordRecipe
 * - crée et enregistre la recette (fallback simple)
 * - intercepte le craft et donne l'épée custom via /give
 */
public class DragonSwordRecipe implements Listener {

    private final Main plugin;
    private final NamespacedKey recipeKey;      // clé unique pour la recette plugin
    private final NamespacedKey dragonSwordKey; // clé PDC pour l'item (plugin namespace)

    public DragonSwordRecipe(Main plugin) {
        this.plugin = plugin;

        this.recipeKey = new NamespacedKey(plugin, "dragon_sword_recipe");
        this.dragonSwordKey = new NamespacedKey(plugin, "dragon_sword");

        // enregistre la recette (fallback = DIAMOND_SWORD)
        registerRecipe();

        // on enregistre ce listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("DragonSwordRecipe initialised (key=" + recipeKey + ")");
    }

    /**
     * Enregistre la recette vanilla (fallback simple, sans NBT exotique)
     */
    private void registerRecipe() {
        ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, fallback);
        recipe.shape("BDB", "BHB", "BSB");
        recipe.setIngredient('B', Material.DRAGON_BREATH);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        boolean already = false;
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext();) {
            Recipe r = it.next();
            try {
                java.lang.reflect.Method m = r.getClass().getMethod("getKey");
                Object keyObj = m.invoke(r);
                if (keyObj != null && keyObj.toString().contains(recipeKey.getKey())) {
                    already = true;
                    break;
                }
            } catch (Exception ex) {
                // ignore
            }
        }

        if (!already) {
            Bukkit.addRecipe(recipe);
        }
    }

    /**
     * Intercepte la preview du craft -> on montre une simple épée vanilla (fallback).
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        if (event.getRecipe() instanceof ShapedRecipe shaped
                && shaped.getKey().equals(recipeKey)) {
            // affiche juste une épée vanilla en preview
            ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta meta = fallback.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");
                fallback.setItemMeta(meta);
            }
            event.getInventory().setResult(fallback);
        }
    }

    /**
     * Quand le joueur termine réellement le craft, on annule le résultat
     * et on exécute un /give avec custom_model_data et PDC.
     */
@EventHandler
public void onCraftItem(CraftItemEvent event) {
    if (event.getRecipe() instanceof ShapedRecipe shaped
            && shaped.getKey().equals(recipeKey)) {

        Player player = (Player) event.getWhoClicked();

        // annule le craft normal
        event.setCurrentItem(null);

        // --- 1) Donne l'épée via /give avec le bon rendu (client)
        String giveCmd =
            "give " + player.getName() +
            " diamond_sword[" +
            "minecraft:custom_model_data={strings:[\"dragon_sword\"]}," +
            "minecraft:custom_name='{\"text\":\"Épée du Dragon\",\"color\":\"light_purple\"}'] 1";

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);

        // --- 2) On attend un tick puis on ajoute le PDC côté serveur
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // on cherche la dernière épée donnée dans l'inventaire du joueur
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.DIAMOND_SWORD) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        // Vérifie si c'est bien l'épée du Dragon (via nom par ex.)
                        if (meta.hasDisplayName() && meta.getDisplayName().contains("Épée du Dragon")) {
                            // Ajoute le PDC plugin
                            meta.getPersistentDataContainer().set(
                                dragonSwordKey,
                                PersistentDataType.STRING,
                                "true"
                            );
                            item.setItemMeta(meta);
                            plugin.getLogger().info("Ajout du PDC sur l'épée du Dragon de " + player.getName());
                            break; // on s'arrête après la première trouvée
                        }
                    }
                }
            }
        }, 1L); // 1 tick après le give
    }
}

}
