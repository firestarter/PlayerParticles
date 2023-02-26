package dev.esophose.playerparticles.styles;

import dev.esophose.playerparticles.PlayerParticles;
import dev.esophose.playerparticles.config.CommentedFileConfiguration;
import dev.esophose.playerparticles.manager.DataManager;
import dev.esophose.playerparticles.manager.ParticleManager;
import dev.esophose.playerparticles.particles.PParticle;
import dev.esophose.playerparticles.particles.PPlayer;
import dev.esophose.playerparticles.particles.ParticlePair;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParticleStyleHit extends ConfiguredParticleStyle implements Listener {

    private int multiplier;

    protected ParticleStyleHit() {
        super("hit", false, false, 0);
    }

    @Override
    public List<PParticle> getParticles(ParticlePair particle, Location location) {
        List<PParticle> particles = new ArrayList<>();

        for (int i = 0; i < this.multiplier; i++)
            particles.addAll(DefaultStyles.NORMAL.getParticles(particle, location));

        return particles;
    }

    @Override
    public void updateTimers() {

    }

    @Override
    protected List<String> getGuiIconMaterialNames() {
        return Collections.singletonList("AMETHYST_SHARD");
    }

    @Override
    public boolean hasLongRangeVisibility() {
        return true;
    }

    @Override
    protected void setDefaultSettings(CommentedFileConfiguration config) {
        this.setIfNotExists("multiplier", 15, "The multiplier for the number of particles to spawn", "This style uses the same spawning as the 'normal' style");
    }

    @Override
    protected void loadSettings(CommentedFileConfiguration config) {
        this.multiplier = config.getInt("multiplier");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void preAttack(PrePlayerAttackEntityEvent event) {
        handleInteraction(event.getPlayer(), event.getAttacked());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDamageEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            handleInteraction(attacker, event.getEntity());
        }
    }

    private void handleInteraction(Player attacker, Entity attacked) {
        ParticleManager particleManager = PlayerParticles.getInstance().getManager(ParticleManager.class);

        if (attacked instanceof LivingEntity entity) {
            PPlayer pplayer = PlayerParticles.getInstance().getManager(DataManager.class).getPPlayer(attacker.getUniqueId());

            if (pplayer == null) {
                return;
            }

            for (ParticlePair particle : pplayer.getActiveParticlesForStyle(DefaultStyles.SWORDS)) {
                Location loc = entity.getLocation().add(0, 1, 0);
                particleManager.displayParticles(pplayer, attacker.getWorld(), particle, DefaultStyles.SWORDS.getParticles(particle, loc), false);
            }
        }
    }
}