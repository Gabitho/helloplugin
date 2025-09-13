package com.pvpheads.recipes;

import com.pvpheads.main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.persistence.PersistentDataType;

/**
 * Gestion de la recette et de l'objet "Épée du Dragon"
 */
public class DragonSwordRecipe implements Listener {

    private final main plugin; // référence vers la classe principale
    private final NamespacedKey recipeKey; // clé unique pour la recette
    private final NamespacedKey dragonSwordKey; // clé PDC pour identifier l’épée

    // ⚡ L’objet modèle, créé une seule fois au démarrage
    private final ItemStack dragonSwordTemplate;

    public DragonSwordRecipe(main plugin) {
        this.plugin = plugin;

        // on génère des clés uniques pour notre plugin
        this.recipeKey = new NamespacedKey(plugin, "dragon_sword_recipe");
        this.dragonSwordKey = new NamespacedKey(plugin, "dragon_sword");

        // on crée l’épée "modèle" une seule fois
        this.dragonSwordTemplate = createDragonSword();

        // on enregistre la recette
        registerRecipe();

        // on enregistre cette classe comme listener (pour intercepter le craft)
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Crée l’item "Épée du Dragon" avec CustomModelData (string) + PDC plugin
     */
    private ItemStack createDragonSword() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);

        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            // Nom custom
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");

            // CustomModelData en string (utilisé par ton pack)
            meta.getPersistentDataContainer().set(
                    new NamespacedKey("minecraft", "custom_model_data"),
                    PersistentDataType.STRING,
                    "dragon_sword"
            );

            // PDC spécifique à ton plugin → permet de détecter facilement l’arme
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
     * Déclare la recette de craft (utilise un "fallback" pour l’aperçu)
     */
    private void registerRecipe() {
        // fallback = épée diamant normale (remplacée ensuite dans PrepareItemCraftEvent)
        ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);

        // Recette shapée
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, fallback);
        recipe.shape("BDB", "BHB", "BSB");

        // B = Dragon’s Breath
        recipe.setIngredient('B', Material.DRAGON_BREATH);
        // D = Dragon Egg
        recipe.setIngredient('D', Material.DRAGON_EGG);
        // H = Player Head
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        // S = Diamond Sword
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        // enregistrement auprès de Bukkit
        Bukkit.addRecipe(recipe);
    }

    /**
     * Intercepte l’événement de "preview" du craft
     * → permet de remplacer le fallback par l’item custom
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        // on vérifie que la recette correspond à la nôtre
        if (event.getRecipe() instanceof ShapedRecipe shaped
                && shaped.getKey().equals(recipeKey)) {

            // ✅ clone du modèle → garde le nom, le PDC et le custom model data
            event.getInventory().setResult(dragonSwordTemplate.clone());
        }
    }
}
