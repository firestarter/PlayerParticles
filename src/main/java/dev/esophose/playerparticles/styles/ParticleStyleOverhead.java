package dev.esophose.playerparticles.styles;

import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.esophose.playerparticles.particles.PParticle;
import dev.esophose.playerparticles.particles.ParticlePair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;

public class ParticleStyleOverhead extends ConfiguredParticleStyle {

    private double headOffset;
    private double particleSpreadX, particleSpreadY, particleSpreadZ;
    private double particleSpeed;
    private int particlesPerTick;

    protected ParticleStyleOverhead() {
        super("overhead", true, false, -0.5);
    }

    @Override
    public List<PParticle> getParticles(ParticlePair particle, Location location) {
        List<PParticle> particles = new ArrayList<>();

        for (int i = 0; i < this.particlesPerTick; i++)
            particles.add(PParticle.builder(location.clone().add(0, this.headOffset, 0)).offsets(this.particleSpreadX, this.particleSpreadY, this.particleSpreadZ).speed(this.particleSpeed).build());

        return particles;
    }

    @Override
    public void updateTimers() {

    }

    @Override
    protected List<String> getGuiIconMaterialNames() {
        return Collections.singletonList("GLOWSTONE");
    }

    @Override
    protected void setDefaultSettings(CommentedFileConfiguration config) {
        this.setIfNotExists("head-offset", 1.75, "How far to offset the player location vertically");
        this.setIfNotExists("particle-spread-x", 0.4, "How far to spread the particles on the x-axis");
        this.setIfNotExists("particle-spread-y", 0.1, "How far to spread the particles on the y-axis");
        this.setIfNotExists("particle-spread-z", 0.4, "How far to spread the particles on the z-axis");
        this.setIfNotExists("particle-speed", 0.0, "The speed of the particles");
        this.setIfNotExists("particles-per-tick", 1, "How many particles to spawn per tick");
    }

    @Override
    protected void loadSettings(CommentedFileConfiguration config) {
        this.headOffset = config.getDouble("head-offset");
        this.particleSpreadX = config.getDouble("particle-spread-x");
        this.particleSpreadY = config.getDouble("particle-spread-y");
        this.particleSpreadZ = config.getDouble("particle-spread-z");
        this.particleSpeed = config.getDouble("particle-speed");
        this.particlesPerTick = config.getInt("particles-per-tick");
    }

}
