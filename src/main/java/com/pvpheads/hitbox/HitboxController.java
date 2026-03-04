package com.pvpheads.hitbox;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * HitboxController :
 * - créé une Interaction entity
 * - configure sa taille
 * - expose des méthodes pour lire les interactions/attaques
 */
public class HitboxController {

    private final Interaction hitbox;
    private final Plugin plugin;

    public HitboxController(Plugin plugin, World world, Location spawn,
                            float width, float height) {

        this.plugin = plugin;
        this.hitbox = (Interaction) world.spawnEntity(spawn, EntityType.INTERACTION);

        // taille de hitbox (en blocs)
        hitbox.setInteractionWidth(width);
        hitbox.setInteractionHeight(height);

        // responsive → nécessaire pour que getLastInteraction() / getLastAttack() puissent être remplis
        hitbox.setResponsive(true);
    }

    public Interaction getHitboxEntity() {
        return hitbox;
    }

    /**
     * Lit la dernière interaction qui a eu lieu sur cette hitbox.
     * Si quelqu'un a interagi récemment, retourne l'UUID du joueur.
     * Sinon retourne null.
     */
    public UUID readLastInteraction() {
        Interaction.PreviousInteraction prev = hitbox.getLastInteraction();
        if (prev != null) {
            return prev.getPlayer().getUniqueId();
        }
        return null;
    }

    /**
     * Lit la dernière attaque sur cette hitbox.
     * Retourne l'UUID si présent, sinon null.
     */
    public UUID readLastAttack() {
        Interaction.PreviousInteraction prev = hitbox.getLastAttack();
        if (prev != null) {
            return prev.getPlayer().getUniqueId();
        }
        return null;
    }

    /**
     * Vérifie et vide l'interaction signalée.
     * Utile pour éviter de relire encore et encore la même interaction.
     */
    public void resetLastInteraction() {
        // Attention : Paper ne propose pas de setter direct pour effacer !
        // Si vous voulez empêcher la réutilisation, vous pouvez stocker le dernier
        // UUID que vous avez vu et comparer ensuite.
    }

    /** Déplace la hitbox à une nouvelle position. */
    public void teleport(Location to) {
        hitbox.teleport(to);
    }

    /** Supprime proprement la hitbox. */
    public void remove() {
        hitbox.remove();
    }
}