package dev.esophose.playerparticles.command;

import dev.esophose.playerparticles.PlayerParticles;
import dev.esophose.playerparticles.api.PlayerParticlesAPI;
import dev.esophose.playerparticles.manager.LocaleManager;
import dev.esophose.playerparticles.manager.ParticleGroupPresetManager;
import dev.esophose.playerparticles.manager.PermissionManager;
import dev.esophose.playerparticles.particles.PPlayer;
import dev.esophose.playerparticles.particles.ParticleGroup;
import dev.esophose.playerparticles.particles.ParticlePair;
import dev.esophose.playerparticles.particles.preset.ParticleGroupPreset;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.util.StringUtil;

public class GroupCommandModule implements CommandModule {

    private static final List<String> VALID_COMMANDS = Arrays.asList("save", "load", "unload", "remove", "info", "list");

    @Override
    public void onCommandExecute(PPlayer pplayer, String[] args) {
        LocaleManager localeManager = PlayerParticles.getInstance().getManager(LocaleManager.class);

        if (args.length == 0 || !VALID_COMMANDS.contains(args[0])) {
            localeManager.sendMessage(pplayer, "command-description-group-save");
            localeManager.sendMessage(pplayer, "command-description-group-load");
            localeManager.sendMessage(pplayer, "command-description-group-unload");
            localeManager.sendMessage(pplayer, "command-description-group-remove");
            localeManager.sendMessage(pplayer, "command-description-group-info");
            localeManager.sendMessage(pplayer, "command-description-group-list");
            return;
        }
        
        if (args.length == 1 && !args[0].equalsIgnoreCase("list")) {
            localeManager.sendMessage(pplayer, "group-no-name", StringPlaceholders.single("cmd", args[0].toLowerCase()));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "save":
                this.onSave(pplayer, args[1].toLowerCase());
                break;
            case "load":
                this.onLoad(pplayer, args[1].toLowerCase());
                break;
            case "unload":
                this.onUnload(pplayer, args[1].toLowerCase());
                break;
            case "remove":
                this.onRemove(pplayer, args[1].toLowerCase());
                break;
            case "info":
                this.onInfo(pplayer, args[1].toLowerCase());
                break;
            case "list":
                this.onList(pplayer);
                break;
            default:
                localeManager.sendMessage(pplayer, "command-description-group-save");
                localeManager.sendMessage(pplayer, "command-description-group-load");
                localeManager.sendMessage(pplayer, "command-description-group-unload");
                localeManager.sendMessage(pplayer, "command-description-group-remove");
                localeManager.sendMessage(pplayer, "command-description-group-info");
                localeManager.sendMessage(pplayer, "command-description-group-list");
                break;
        }
    }

    /**
     * Handles the command /pp group save
     * 
     * @param pplayer The PPlayer
     * @param groupName The target group name
     */
    private void onSave(PPlayer pplayer, String groupName) {
        LocaleManager localeManager = PlayerParticles.getInstance().getManager(LocaleManager.class);

        // Check that the groupName isn't the reserved name
        if (groupName.equalsIgnoreCase(ParticleGroup.DEFAULT_NAME)) {
            localeManager.sendMessage(pplayer, "group-reserved");
            return;
        }
        
        // Check if the player actually has any particles
        if (pplayer.getActiveParticles().size() == 0) {
            localeManager.sendMessage(pplayer, "group-save-no-particles");
            return;
        }
        
        // The database column can only hold up to 100 characters, cut it off there
        if (groupName.length() >= 100) {
            groupName = groupName.substring(0, 100);
        }
        
        // Check if they are creating a new group, if they are, check that they haven't gone over their limit
        boolean groupUpdated = pplayer.getParticleGroupByName(groupName) != null;
        if (!groupUpdated && PlayerParticles.getInstance().getManager(PermissionManager.class).hasPlayerReachedMaxGroups(pplayer)) {
            localeManager.sendMessage(pplayer, "group-save-reached-max");
            return;
        }
        
        // Use the existing group if available, otherwise create a new one
        Map<Integer, ParticlePair> particles = new ConcurrentHashMap<>();
        for (ParticlePair particle : pplayer.getActiveParticles())
            particles.put(particle.getId(), particle.clone()); // Make sure the ParticlePairs aren't the same references in both the active and saved group
        ParticleGroup group = new ParticleGroup(groupName, particles);
        
        // Apply changes and notify player
        PlayerParticlesAPI.getInstance().savePlayerParticleGroup(pplayer.getPlayer(), group);
        if (groupUpdated) {
            localeManager.sendMessage(pplayer, "group-save-success-overwrite", StringPlaceholders.single("name", groupName));
        } else {
            localeManager.sendMessage(pplayer, "group-save-success", StringPlaceholders.single("name", groupName));
        }
    }
    
    /**
     * Handles the command /pp group load
     * 
     * @param pplayer The PPlayer
     * @param groupName The target group name
     */
    private void onLoad(PPlayer pplayer, String groupName) {
        LocaleManager localeManager = PlayerParticles.getInstance().getManager(LocaleManager.class);

        // Check that the groupName isn't the reserved name
        if (groupName.equalsIgnoreCase(ParticleGroup.DEFAULT_NAME)) {
            localeManager.sendMessage(pplayer, "group-reserved");
            return;
        }
        
        // Get the group
        ParticleGroup group = pplayer.getParticleGroupByName(groupName);
        if (group != null) {
            if (!group.canPlayerUse(pplayer)) {
                localeManager.sendMessage(pplayer, "group-no-permission", StringPlaceholders.single("group", groupName));
                return;
            }

            // Empty out the active group and fill it with clones from the target group
            ParticleGroup activeGroup = pplayer.getActiveParticleGroup();
            activeGroup.getParticles().clear();
            for (ParticlePair particle : group.getParticles().values()) {
                ParticlePair clonedParticle = particle.clone();
                clonedParticle.setOwner(pplayer);
                activeGroup.getParticles().put(clonedParticle.getId(), clonedParticle);
            }

            // Update group and notify player
            PlayerParticlesAPI.getInstance().savePlayerParticleGroup(pplayer.getPlayer(), activeGroup);
            localeManager.sendMessage(pplayer, "group-load-success", StringPlaceholders.builder("amount", activeGroup.getParticles().size()).addPlaceholder("name", groupName).build());
            return;
        }

        // Didn't find a saved group, look at the presets
        ParticleGroupPreset presetGroup = PlayerParticles.getInstance().getManager(ParticleGroupPresetManager.class).getPresetGroup(groupName);
        if (presetGroup == null) {
            localeManager.sendMessage(pplayer, "group-invalid", StringPlaceholders.single("name", groupName));
            return;
        }

        if (!presetGroup.canPlayerUse(pplayer)) {
            localeManager.sendMessage(pplayer, "group-preset-no-permission", StringPlaceholders.single("group", groupName));
            return;
        }

        pplayer.loadPresetGroup(presetGroup.getGroup().getParticles().values());
        localeManager.sendMessage(pplayer, "group-load-preset-success", StringPlaceholders.builder("amount", presetGroup.getGroup().getParticles().size()).addPlaceholder("name", groupName).build());
    }

    /**
     * Handles the command /pp group unload
     *
     * @param pplayer The PPlayer
     * @param groupName The target group name
     */
    private void onUnload(PPlayer pplayer, String groupName) {
        LocaleManager localeManager = PlayerParticles.getInstance().getManager(LocaleManager.class);

        // Check that the groupName isn't the reserved name
        if (groupName.equalsIgnoreCase(ParticleGroup.DEFAULT_NAME)) {
            localeManager.sendMessage(pplayer, "group-reserved");
            return;
        }

        // Get the group
        ParticleGroup group = pplayer.getParticleGroupByName(groupName);
        if (group != null) {
            if (!group.canPlayerUse(pplayer)) {
                localeManager.sendMessage(pplayer, "group-no-permission", StringPlaceholders.single("group", groupName));
                return;
            }

            // Remove all particles that match the group
            AtomicInteger matches = new AtomicInteger(0);
            ParticleGroup activeGroup = pplayer.getActiveParticleGroup();
            for (ParticlePair particle : group.getParticles().values()) {
                activeGroup.getParticles().values().removeIf(x -> {
                    boolean match = x.getEffect() == particle.getEffect() && x.getStyle() == particle.getStyle();
                    if (match) matches.incrementAndGet();
                    return match;
                });
            }

            // Update group and notify player
            PlayerParticlesAPI.getInstance().savePlayerParticleGroup(pplayer.getPlayer(), activeGroup);
            localeManager.sendMessage(pplayer, "group-unload-success", StringPlaceholders.builder("amount", matches.get()).addPlaceholder("name", groupName).build());
            return;
        }

        // Didn't find a saved group, look at the presets
        ParticleGroupPreset presetGroup = PlayerParticles.getInstance().getManager(ParticleGroupPresetManager.class).getPresetGroup(groupName);
        if (presetGroup == null) {
            localeManager.sendMessage(pplayer, "group-invalid", StringPlaceholders.single("name", groupName));
            return;
        }

        if (!presetGroup.canPlayerUse(pplayer)) {
            localeManager.sendMessage(pplayer, "group-preset-no-permission", StringPlaceholders.single("group", groupName));
            return;
        }

        // Remove all particles that match the group
        AtomicInteger matches = new AtomicInteger(0);
        ParticleGroup activeGroup = pplayer.getActiveParticleGroup();
        for (ParticlePair particle : presetGroup.getGroup().getParticles().values()) {
            activeGroup.getParticles().values().removeIf(x -> {
                boolean match = x.getEffect() == particle.getEffect() && x.getStyle() == particle.getStyle();
                if (match) matches.incrementAndGet();
                return match;
            });
        }

        PlayerParticlesAPI.getInstance().savePlayerParticleGroup(pplayer.getPlayer(), activeGroup);
        localeManager.sendMessage(pplayer, "group-unload-success", StringPlaceholders.builder("amount", matches.get()).addPlaceholder("name", groupName).build());
    }
    
    /**
     * Handles the command /pp group remove
     * 
     * @param pplayer The PPlayer
     * @param groupName The target group name
     */
    private void onRemove(PPlayer pplayer, String groupName) {
        LocaleManager localeManager = PlayerParticles.getInstance().getManager(LocaleManager.class);

        // Check that the groupName isn't the reserved name
        if (groupName.equalsIgnoreCase(ParticleGroup.DEFAULT_NAME)) {
            localeManager.sendMessage(pplayer, "group-reserved");
            return;
        }
        
        ParticleGroup group = pplayer.getParticleGroupByName(groupName);
        if (group == null) {
            // Didn't find a saved group, look at the presets
            ParticleGroupPreset presetGroup = PlayerParticles.getInstance().getManager(ParticleGroupPresetManager.class).getPresetGroup(groupName);
            
            if (presetGroup == null) {
                localeManager.sendMessage(pplayer, "group-invalid", StringPlaceholders.single("name", groupName));
            } else {
                localeManager.sendMessage(pplayer, "group-remove-preset");
            }
            return;
        }
        
        // Delete the group and notify player
        PlayerParticlesAPI.getInstance().removePlayerParticleGroup(pplayer.getPlayer(), group.getName());
        localeManager.sendMessage(pplayer, "group-remove-success", StringPlaceholders.single("name", groupName));
    }
    
    /**
     * Handles the command /pp group info
     * 
     * @param pplayer The PPlayer
     * @param groupName The target group name
     */
    private void onInfo(PPlayer pplayer, String groupName) {
        LocaleManager localeManager = PlayerParticles.getInstance().getManager(LocaleManager.class);

        // Check that the groupName isn't the reserved name
        if (groupName.equalsIgnoreCase(ParticleGroup.DEFAULT_NAME)) {
            localeManager.sendMessage(pplayer, "group-reserved");
            return;
        }
        
        ParticleGroup group = pplayer.getParticleGroupByName(groupName);
        if (group == null) {
            // Didn't find a saved group, look at the presets
            ParticleGroupPreset presetGroup = PlayerParticles.getInstance().getManager(ParticleGroupPresetManager.class).getPresetGroup(groupName);
            if (presetGroup == null) {
                localeManager.sendMessage(pplayer, "group-invalid", StringPlaceholders.single("name", groupName));
                return;
            }
            
            if (!presetGroup.canPlayerUse(pplayer)) {
                localeManager.sendMessage(pplayer, "group-preset-no-permission", StringPlaceholders.single("group", groupName));
                return;
            }
            
            group = presetGroup.getGroup();
        }
        
        List<ParticlePair> particles = new ArrayList<>(group.getParticles().values());
        particles.sort(Comparator.comparingInt(ParticlePair::getId));
        
        localeManager.sendMessage(pplayer, "group-info-header", StringPlaceholders.single("group", groupName));
        for (ParticlePair particle : particles) {
            StringPlaceholders stringPlaceholders = StringPlaceholders.builder("id", particle.getId())
                    .addPlaceholder("effect", particle.getEffect().getName())
                    .addPlaceholder("style", particle.getStyle().getName())
                    .addPlaceholder("data", particle.getDataString())
                    .build();
            localeManager.sendMessage(pplayer, "list-output", stringPlaceholders);
        }
    }
    
    /**
     * Handles the command /pp group list
     * 
     * @param pplayer The PPlayer
     */
    private void onList(PPlayer pplayer) {
        LocaleManager localeManager = PlayerParticles.getInstance().getManager(LocaleManager.class);
        
        List<ParticleGroup> groups = new ArrayList<>(pplayer.getParticleGroups().values());
        groups.sort(Comparator.comparing(ParticleGroup::getName));

        StringBuilder groupsList = new StringBuilder();
        for (ParticleGroup group : groups)
            if (!group.getName().equals(ParticleGroup.DEFAULT_NAME))
                groupsList.append(group.getName()).append(", ");
        
        if (groupsList.toString().endsWith(", "))
            groupsList = new StringBuilder(groupsList.substring(0, groupsList.length() - 2));
        
        StringBuilder presetsList = new StringBuilder();
        for (ParticleGroupPreset group : PlayerParticles.getInstance().getManager(ParticleGroupPresetManager.class).getPresetGroupsForPlayer(pplayer))
            presetsList.append(group.getGroup().getName()).append(", ");
        
        if (presetsList.toString().endsWith(", "))
            presetsList = new StringBuilder(presetsList.substring(0, presetsList.length() - 2));
        
        if ((groupsList.length() == 0) && (presetsList.length() == 0)) {
            localeManager.sendMessage(pplayer, "group-list-none");
            return;
        }
        
        if (groupsList.length() > 0) {
            localeManager.sendMessage(pplayer, "group-list-output", StringPlaceholders.single("info", groupsList.toString()));
        }
        
        if (presetsList.length() > 0) {
            localeManager.sendMessage(pplayer, "group-list-presets", StringPlaceholders.single("info", presetsList.toString()));
        }
    }

    @Override
    public List<String> onTabComplete(PPlayer pplayer, String[] args) {
        List<String> matches = new ArrayList<>();
        if (args.length <= 1) {
            if (args.length == 0) matches = VALID_COMMANDS;
            else StringUtil.copyPartialMatches(args[0], VALID_COMMANDS, matches);
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("list")) {
            if (args[0].equalsIgnoreCase("save")) {
                matches.add("<groupName>");
            } else {
                List<String> groupNames = new ArrayList<>();
                for (ParticleGroup group : pplayer.getParticleGroups().values())
                    if (!group.getName().equals(ParticleGroup.DEFAULT_NAME))
                        groupNames.add(group.getName());
                if (!args[0].equals("remove"))
                    for (ParticleGroupPreset group : PlayerParticles.getInstance().getManager(ParticleGroupPresetManager.class).getPresetGroupsForPlayer(pplayer))
                        groupNames.add(group.getGroup().getName());
                StringUtil.copyPartialMatches(args[1], groupNames, matches);
            }
        }
        
        return matches;
    }

    @Override
    public String getName() {
        return "group";
    }

    @Override
    public String getDescriptionKey() {
        return "command-description-group";
    }

    @Override
    public String getArguments() {
        return "<sub-command>";
    }

    @Override
    public boolean requiresEffectsAndStyles() {
        return false;
    }

    @Override
    public boolean canConsoleExecute() {
        return false;
    }

}
