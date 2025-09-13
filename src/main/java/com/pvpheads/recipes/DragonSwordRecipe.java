package com.pvpheads.recipes;

import com.pvpheads.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.ChatColor;

public class DragonSwordRecipe {

    private final Main plugin;

    public DragonSwordRecipe(Main plugin) {
        this.plugin = plugin;
        register();
    }

    public void register() {
        plugin.getLogger().info("Enregistrement de la recette 'dragon_sword'...");

        // ✅ Créer un ItemStack valide comme résultat
        ItemStack result = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            plugin.getLogger().severe("ERREUR: Impossible de récupérer l'ItemMeta du résultat !");
            return;
        }

        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");
        meta.setCustomModelData(1); // Custom model data pour ton pack de textures
        result.setItemMeta(meta);

        // ✅ Créer la clé Namespaced
        NamespacedKey key = new NamespacedKey(plugin, "dragon_sword");
        plugin.getLogger().info("NamespacedKey créé: " + key.toString());

        // ✅ Définir la recette
        ShapedRecipe recipe = new ShapedRecipe(key, result);

        recipe.shape(
            "BDB",
            "BHB",
            "BSB"
        );

        recipe.setIngredient('B', Material.DRAGON_BREATH);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        // ✅ Ajouter la recette
        boolean success = Bukkit.addRecipe(recipe);
        if (success) {
            plugin.getLogger().info("Recette 'dragon_sword' enregistrée avec succès !");
        } else {
            plugin.getLogger().severe("ÉCHEC lors de l'enregistrement de la recette 'dragon_sword'.");
        }
    }
}
