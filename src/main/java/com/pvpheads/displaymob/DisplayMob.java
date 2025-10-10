package com.pvpheads.displaymob;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DisplayMob - version passive
 * - plusieurs ItemDisplay pour le visuel
 * - une ou plusieurs Slime comme hitboxes (passives : setAI(false))
 * - animation légère (pulsation de scale) et repositionnement régulier des ItemDisplay
 * - gestion de PV via applyDamage(...)
 */
public class DisplayMob {

    private final Plugin plugin;
    private final UUID mobId;
    private final NamespacedKey pdcKey;

    // Visual parts
    private final List<ItemDisplay> visualParts = new ArrayList<>();
    private final List<Float> visualBaseScales = new ArrayList<>(); // base scale par part

    // Hitboxes (slimes)
    private final List<Slime> hitboxEntities = new ArrayList<>();

    private BukkitTask animationTask;

    // Health
    private final int maxHealth;
    private int currentHealth;

    public DisplayMob(Plugin plugin, int maxHealth) {
        this.plugin = plugin;
        this.mobId = UUID.randomUUID();
        this.pdcKey = new NamespacedKey(plugin, "display_mob_id");
        this.maxHealth = Math.max(1, maxHealth);
        this.currentHealth = this.maxHealth;
    }

    public UUID getId() { return mobId; }

    /**
     * Spawn the passive mob.
     *
     * @param center spawn center
     * @param visualScale multiplicateur visuel (1.0 = normal)
     * @param mainHitboxSize slime size (1..)
     */
    public void spawn(Location center, float visualScale, int mainHitboxSize) {
        World world = center.getWorld();
        if (world == null) return;

        // --- Main hitbox slime (passive) ---
        Slime mainHitbox = (Slime) world.spawnEntity(center.clone().add(0, 0.5, 0), EntityType.SLIME);
        mainHitbox.setSize(Math.max(1, mainHitboxSize));
        mainHitbox.setAI(false);        // passive => pas d'IA
        mainHitbox.setSilent(true);
        mainHitbox.setGravity(false);
        mainHitbox.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, mobId.toString());
        hitboxEntities.add(mainHitbox);

        // --- Visual part : body ---
        ItemStack bodyStack = new ItemStack(Material.DIAMOND); // placeholder: remplace par ton ItemStack custom (CMD)
        ItemDisplay bodyDisplay = (ItemDisplay) world.spawnEntity(center.clone().add(0, 0.5, 0), EntityType.ITEM_DISPLAY);
        bodyDisplay.setItemStack(bodyStack);
        bodyDisplay.setBillboard(Display.Billboard.CENTER);
        bodyDisplay.setVisibleByDefault(true);
        bodyDisplay.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, mobId.toString());

        // display size + transformation (scale)
        float bodyBaseSize = 1.0f * visualScale;
        bodyDisplay.setDisplayWidth(bodyBaseSize);
        bodyDisplay.setDisplayHeight(bodyBaseSize);
        Vector3f bodyTranslation = new Vector3f(0f, 0.5f, 0f);
        Quaternionf identityRotation = new Quaternionf();
        Vector3f bodyScale = new Vector3f(visualScale, visualScale, visualScale);
        Transformation bodyTransform = new Transformation(bodyTranslation, identityRotation, bodyScale, new Quaternionf());
        bodyDisplay.setTransformation(bodyTransform);

        visualParts.add(bodyDisplay);
        visualBaseScales.add(visualScale);

        // --- Visual part : head (smaller) ---
        ItemStack headStack = new ItemStack(Material.DIAMOND_SWORD); // placeholder
        ItemDisplay headDisplay = (ItemDisplay) world.spawnEntity(center.clone().add(0, 1.2, 0), EntityType.ITEM_DISPLAY);
        headDisplay.setItemStack(headStack);
        headDisplay.setBillboard(Display.Billboard.CENTER);
        headDisplay.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, mobId.toString());

        float headBaseVisual = 0.6f * visualScale;
        headDisplay.setDisplayWidth(headBaseVisual);
        headDisplay.setDisplayHeight(headBaseVisual);
        Vector3f headTranslation = new Vector3f(0f, 1.2f, 0f);
        Vector3f headScale = new Vector3f(visualScale * 0.9f, visualScale * 0.9f, visualScale * 0.9f);
        headDisplay.setTransformation(new Transformation(headTranslation, identityRotation, headScale, new Quaternionf()));

        visualParts.add(headDisplay);
        visualBaseScales.add(visualScale * 0.9f);

        // --- Head hitbox (passive) ---
        Slime headHitbox = (Slime) world.spawnEntity(center.clone().add(0, 1.2, 0), EntityType.SLIME);
        headHitbox.setSize(Math.max(1, (int) Math.max(1, mainHitboxSize / 2f)));
        headHitbox.setAI(false);
        headHitbox.setSilent(true);
        headHitbox.setGravity(false);
        headHitbox.getPersistentDataContainer().set(pdcKey, PersistentDataType.STRING, mobId.toString());
        hitboxEntities.add(headHitbox);

        // Start animation & follow task (passive, low frequency for perf)
        startAnimationAndFollowTask();
    }

    private void startAnimationAndFollowTask() {
        // tick every 2 ticks for perf; you can set to 1L for smoother animation
        animationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (hitboxEntities.isEmpty()) {
                remove();
                return;
            }
            Slime mainHitbox = hitboxEntities.get(0);
            if (mainHitbox == null || mainHitbox.isDead()) {
                handleDeath(null);
                return;
            }

            // light pulse animation
            float currentTimeSeconds = (System.currentTimeMillis() % 10000L) / 1000f;
            float pulse = 1.0f + 0.03f * (float)Math.sin(currentTimeSeconds * 2.0f);

            Location centerLocation = mainHitbox.getLocation();

            for (int index = 0; index < visualParts.size(); index++) {
                ItemDisplay displayPart = visualParts.get(index);
                if (displayPart == null || displayPart.isDead()) continue;

                float yOffset = (index == 0) ? 0.5f : 1.2f;
                Vector3f translation = new Vector3f(0f, yOffset, 0f);

                // baseScale stored at creation -> apply pulse
                float baseScale = visualBaseScales.get(index);
                Vector3f newScale = new Vector3f(baseScale * pulse, baseScale * pulse, baseScale * pulse);

                Transformation newTransform = new Transformation(translation, new Quaternionf().rotationY(currentTimeSeconds * 0.2f), newScale, new Quaternionf());
                displayPart.teleport(centerLocation.clone().add(0, yOffset, 0));
                displayPart.setTransformation(newTransform);
                displayPart.setInterpolationDuration(6); // smooth transition
            }

            // keep hitboxes at center (prevent drift)
            for (Slime hitbox : hitboxEntities) {
                if (hitbox == null || hitbox.isDead()) continue;
                hitbox.teleport(centerLocation.clone().add(0, 0.0, 0.0));
            }
        }, 0L, 2L);
    }

    /**
     * Apply damage to the mob (called from the listener).
     * We manage HP ourselves (passive mob).
     */
    public void applyDamage(int damageAmount, org.bukkit.entity.Entity source) {
        if (damageAmount <= 0) return;

        currentHealth -= damageAmount;
        plugin.getLogger().info("DisplayMob " + mobId + " took " + damageAmount + " damage (hp " + currentHealth + "/" + maxHealth + ")");

        // minor visual knockback: push main hitbox away from damager if possible
        if (!hitboxEntities.isEmpty() && source != null) {
            Slime mainHitbox = hitboxEntities.get(0);
            if (mainHitbox != null && !mainHitbox.isDead()) {
                Vector pushDirection = mainHitbox.getLocation().toVector().subtract(source.getLocation().toVector()).normalize();
                mainHitbox.setVelocity(pushDirection.multiply(0.45).setY(0.2));
            }
        }

        // spawn damage particles
        if (!hitboxEntities.isEmpty()) {
            Slime mainHitbox = hitboxEntities.get(0);
            mainHitbox.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, mainHitbox.getLocation(), 6, 0.2, 0.4, 0.2);
        }

        if (currentHealth <= 0) {
            handleDeath(source);
        }
    }

    private void handleDeath(org.bukkit.entity.Entity killer) {
        plugin.getLogger().info("DisplayMob " + mobId + " died. Cleaning up.");

        if (!hitboxEntities.isEmpty()) {
            Slime mainHitbox = hitboxEntities.get(0);
            mainHitbox.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, mainHitbox.getLocation(), 1, 0.4, 0.4, 0.4);
            mainHitbox.getWorld().playSound(mainHitbox.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            mainHitbox.getWorld().dropItemNaturally(mainHitbox.getLocation(), new ItemStack(Material.DIAMOND)); // exemple de drop
        }

        remove();
    }

    /** Remove everything cleanly. */
    public void remove() {
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }

        for (ItemDisplay displayPart : visualParts) {
            if (displayPart != null && !displayPart.isDead()) displayPart.remove();
        }
        visualParts.clear();
        visualBaseScales.clear();

        for (Slime hitbox : hitboxEntities) {
            if (hitbox != null && !hitbox.isDead()) hitbox.remove();
        }
        hitboxEntities.clear();
    }
}
