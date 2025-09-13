package com.pvpheads.recipes;

import com.pvpheads.Main; // attention à la casse : ta classe principale doit s'appeler Main

import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;

import org.bukkit.persistence.PersistentDataType;

/**
 * DragonSwordRecipe
 * - crée et enregistre la recette (fallback simple)
 * - prépare un "template" ItemStack (PDC + custom model data string)
 * - dans PrepareItemCraftEvent, on remplace le résultat par le clone du template
 */
public class DragonSwordRecipe implements Listener {

    private final Main plugin;
    private final NamespacedKey recipeKey;      // clé unique pour la recette plugin
    private final NamespacedKey dragonSwordKey; // clé PDC pour l'item (plugin namespace)
    private final ItemStack dragonSwordTemplate; // template clonable (meta déjà préparée)

    public DragonSwordRecipe(Main plugin) {
        this.plugin = plugin;

        // clé de recette : évite d'utiliser exactement "dragon_sword" si ça peut exister ailleurs
        this.recipeKey = new NamespacedKey(plugin, "dragon_sword_recipe");
        // clé PDC pour identifier notre item côté plugin
        this.dragonSwordKey = new NamespacedKey(plugin, "dragon_sword");

        // créer le template (avec nom, PDC et custom_model_data string pour le client)
        this.dragonSwordTemplate = createDragonSwordTemplate();

        // enregistre la recette (fallback = DIAMOND_SWORD)
        registerRecipe();

        // on enregistre ce listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("DragonSwordRecipe initialised (key=" + recipeKey + ")");
    }

    /**
     * Crée l'ItemStack template (nom, PDC plugin, et custom_model_data as string pour le resource pack)
     * IMPORTANT : on n'utilise PAS cet ItemStack directement dans ShapedRecipe pour éviter
     *            que le serveur tente de sérialiser des NBT interdits dans la recette.
     */
    private ItemStack createDragonSwordTemplate() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            // Nom visible
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");

            // ----- custom_model_data as string (client 1.21.4+ / items/select)
            // On écrit la donnée dans l'ItemMeta (namespace minecraft:custom_model_data)
            // Le client les lira si le resource pack utilise "select" on "items/".
            meta.getPersistentDataContainer().set(
                new NamespacedKey("minecraft", "custom_model_data"),
                PersistentDataType.STRING,
                "dragon_sword"
            );

            // ----- PDC plugin pour détection côté serveur
            meta.getPersistentDataContainer().set(
                dragonSwordKey,
                PersistentDataType.STRING,
                "true"
            );

            sword.setItemMeta(meta);
        }
        return sword;
    }

    /**
     * Enregistre la recette vanilla (fallback simple, sans NBT exotique)
     */
    private void registerRecipe() {
        // fallback : DIAMOND_SWORD simple (aucune méta spéciale ici)
        ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);

        // construit la recipe
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, fallback);
        recipe.shape("BDB", "BHB", "BSB");
        recipe.setIngredient('B', Material.DRAGON_BREATH);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        // Avant d'ajouter : vérifie qu'il n'existe pas déjà une recette du même key
        boolean already = false;
        for (Iterator<Recipe> it = Bukkit.recipeIterator(); it.hasNext();) {
            Recipe r = it.next();
            // si l'API expose getKey (nouvelle API), on peut comparer ; sinon on ignore
            try {
                java.lang.reflect.Method m = r.getClass().getMethod("getKey");
                Object keyObj = m.invoke(r);
                if (keyObj != null && keyObj.toString().contains(recipeKey.getKey())) {
                    already = true;
                    break;
                }
            } catch (Exception ex) {
                // ignore : pas grave si on ne peut pas déterminer la clé via réflexion
            }
        }

        // ADD THE RECIPE TO THE SERVER (this was missing!)
        if (!already) {
            boolean success = Bukkit.addRecipe(recipe);
            if (success) {
                plugin.getLogger().info("Dragon Sword recipe registered successfully!");
            } else {
                plugin.getLogger().warning("Failed to register Dragon Sword recipe!");
            }
        } else {
            plugin.getLogger().info("Dragon Sword recipe already exists, skipping registration.");
        }
    }
    
    
    /**
     * Intercepte la preview du craft et remplace le résultat par notre template cloné.
     * Ainsi le joueur voit l’épée custom et reçoit le template lorsqu’il la prend.
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        if (event.getRecipe() instanceof ShapedRecipe shaped
                && shaped.getKey().equals(recipeKey)) {

            // clone la template (sûr, le joueur obtiendra la meta exacte)
            event.getInventory().setResult(dragonSwordTemplate.clone());
        }
    }
}
