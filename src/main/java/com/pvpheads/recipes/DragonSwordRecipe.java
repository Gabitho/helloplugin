package com.pvpheads.recipes;

import com.pvpheads.Main;

import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.PrepareItemCraftEvent;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.Recipe;

import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * DragonSwordRecipe
 * - crée et enregistre la recette (fallback simple)
 * - prépare un "template" ItemStack (nom + PDC plugin)
 * - intercepte la préparation de craft et exécute un /give vanilla
 */
public class DragonSwordRecipe implements Listener {

    private final Main plugin;
    private final NamespacedKey recipeKey;      // clé unique pour la recette
    private final NamespacedKey dragonSwordKey; // clé PDC pour marquer notre épée
    private final ItemStack dragonSwordTemplate; // template clonable

    public DragonSwordRecipe(Main plugin) {
        this.plugin = plugin;

        this.recipeKey = new NamespacedKey(plugin, "dragon_sword_recipe");
        this.dragonSwordKey = new NamespacedKey(plugin, "dragon_sword");

        // on prépare notre template (nom + PDC)
        this.dragonSwordTemplate = createDragonSwordTemplate();

        // enregistre la recette vanilla fallback
        registerRecipe();

        // enregistre le listener
        Bukkit.getPluginManager().registerEvents(this, plugin);

        plugin.getLogger().info("DragonSwordRecipe initialisé !");
    }

    /**
     * Crée un ItemStack template pour la preview
     */
    private ItemStack createDragonSwordTemplate() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            // Nom lisible
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Épée du Dragon");

            // PDC plugin (pour détection serveur)
            meta.getPersistentDataContainer().set(
                dragonSwordKey,
                PersistentDataType.STRING,
                "true"
            );

            // Optionnel : CustomModelData numérique si tu veux une preview texturée
            // meta.setCustomModelData(1);

            sword.setItemMeta(meta);
        }
        return sword;
    }

    /**
     * Enregistre la recette fallback
     */
    private void registerRecipe() {
        ItemStack fallback = new ItemStack(Material.DIAMOND_SWORD);

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, fallback);
        recipe.shape("BDB", "BHB", "BSB");
        recipe.setIngredient('B', Material.DRAGON_BREATH);
        recipe.setIngredient('D', Material.DRAGON_EGG);
        recipe.setIngredient('H', Material.PLAYER_HEAD);
        recipe.setIngredient('S', Material.DIAMOND_SWORD);

        // vérifie si déjà présent
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
            } catch (Exception ignored) {}
        }

        if (!already) {
            Bukkit.addRecipe(recipe);
            plugin.getLogger().info("✅ Recette DragonSword enregistrée !");
        } else {
            plugin.getLogger().warning("⚠️ Recette DragonSword déjà existante !");
        }
    }

    /**
     * Intercepte la préparation du craft
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;

        if (event.getRecipe() instanceof ShapedRecipe shaped
                && shaped.getKey().equals(recipeKey)) {

            Player player = (Player) event.getView().getPlayer();

            // Met une preview (simple diamant épée renommée)
            event.getInventory().setResult(dragonSwordTemplate.clone());

            // Quand le joueur prend le résultat, on lui donne via /give
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack result = event.getInventory().getResult();
                    if (result != null && result.isSimilar(dragonSwordTemplate)) {

                        // Commande vanilla /give avec custom_model_data en string
                        String giveCmd =
                                "give " + player.getName() +
                                " diamond_sword[" +
                                "minecraft:custom_model_data=\"dragon_sword\"," +
                                "minecraft:custom_name='{\"text\":\"Épée du Dragon\",\"color\":\"light_purple\"}'] 1";

                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);

                        // Petit délai puis ajout du PDC serveur sur l’objet donné
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                ItemStack given = player.getInventory().getItemInMainHand();
                                if (given != null && given.getType() == Material.DIAMOND_SWORD) {
                                    ItemMeta meta = given.getItemMeta();
                                    if (meta != null) {
                                        PersistentDataContainer pdc = meta.getPersistentDataContainer();
                                        pdc.set(dragonSwordKey, PersistentDataType.STRING, "true");
                                        given.setItemMeta(meta);
                                    }
                                }
                            }
                        }.runTaskLater(plugin, 2L);
                    }
                }
            }.runTask(plugin);
        }
    }
}
