package com.pvpheads.recipes;

import com.pvpheads.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;

public class DragonSwordRecipe implements Listener {

    private final Main plugin;
    private final NamespacedKey recipeKey;
    private final NamespacedKey dragonSwordKey;

    public DragonSwordRecipe(Main plugin) {
        this.plugin = plugin;

        this.recipeKey = new NamespacedKey(plugin, "dragon_sword_recipe");
        this.dragonSwordKey = new NamespacedKey(plugin, "dragon_sword");

        registerRecipe();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("DragonSwordRecipe initialised (key=" + recipeKey + ")");
    }

    /**
     * Enregistre la recette vanilla (fallback simple).
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
        for (Iterator<org.bukkit.inventory.Recipe> it = Bukkit.recipeIterator(); it.hasNext();) {
            org.bukkit.inventory.Recipe r = it.next();
            try {
                java.lang.reflect.Method m = r.getClass().getMethod("getKey");
                Object keyObj = m.invoke(r);
                if (keyObj != null && keyObj.toString().contains(recipeKey.getKey())) {
                    already = true;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (!already) {
            Bukkit.addRecipe(recipe);
        }
    }

    /**
     * Intercepte la preview : garde l’épée vanilla en preview.
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        if (event.getRecipe() instanceof ShapedRecipe shaped
                && shaped.getKey().equals(recipeKey)) {
            // juste afficher le fallback vanilla
            event.getInventory().setResult(new ItemStack(Material.DIAMOND_SWORD));
        }
    }

    /**
     * Quand le joueur termine le craft → on supprime le résultat vanilla
     * et on lui donne la version custom via /give (nouvelle syntaxe).
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (event.getRecipe() == null) return;

        if (event.getRecipe() instanceof ShapedRecipe shaped
                && shaped.getKey().equals(recipeKey)) {

            event.setCancelled(true); // annule le craft vanilla

            Player player = (Player) event.getWhoClicked();
            player.closeInventory();

            // --- commande GIVE avec la nouvelle syntaxe (1.21.6)
            String giveCmd =
                "give " + player.getName() +
                " diamond_sword[" +
                "minecraft:custom_model_data=\"dragon_sword\"," +
                "minecraft:custom_name='{\"text\":\"Épée du Dragon\",\"color\":\"light_purple\"}'] 1";

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);

            // --- après un tick, ajoute le PDC plugin à l’objet donné
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack sword = player.getInventory().getItemInMainHand();
                if (sword != null && sword.getType() == Material.DIAMOND_SWORD) {
                    ItemMeta meta = sword.getItemMeta();
                    if (meta != null) {
                        meta.getPersistentDataContainer().set(
                            dragonSwordKey,
                            PersistentDataType.STRING,
                            "true"
                        );
                        sword.setItemMeta(meta);
                    }
                }
            }, 1L);
        }
    }
}
