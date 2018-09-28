package com.esophose.playerparticles.command;

import java.util.List;

import com.esophose.playerparticles.gui.PlayerParticlesGui;
import com.esophose.playerparticles.gui.PlayerParticlesGui.GuiState;
import com.esophose.playerparticles.manager.LangManager;
import com.esophose.playerparticles.manager.PermissionManager;
import com.esophose.playerparticles.manager.LangManager.Lang;
import com.esophose.playerparticles.particles.PPlayer;

public class GUICommandModule implements CommandModule {

    public void onCommandExecute(PPlayer pplayer, String[] args) {
        boolean byDefault = false;
        if (args.length > 0 && args[0].equals("byDefault")) {
            byDefault = true;
        }

        if (PlayerParticlesGui.isGuiDisabled()) {
            if (byDefault) {
                LangManager.sendMessage(pplayer, Lang.INVALID_ARGUMENTS);
            } else {
                LangManager.sendMessage(pplayer, Lang.GUI_DISABLED);
            }
            return;
        }

        if (PermissionManager.getEffectsUserHasPermissionFor(pplayer.getPlayer()).size() == 1) {
            if (byDefault) {
                LangManager.sendMessage(pplayer, Lang.INVALID_ARGUMENTS);
            } else {
                LangManager.sendMessage(pplayer, Lang.NO_PARTICLES);
            }
            return;
        }

        if (byDefault) {
            LangManager.sendMessage(pplayer, Lang.GUI_BY_DEFAULT);
        }

        PlayerParticlesGui.changeState(pplayer, GuiState.DEFAULT);
    }

    public List<String> onTabComplete(PPlayer pplayer, String[] args) {
        return null;
    }

    public String getName() {
        return "gui";
    }

    public String getDescription() {
        return Lang.GUI_COMMAND_DESCRIPTION.get();
    }

    public String getArguments() {
        return "";
    }

    public boolean requiresEffects() {
        return true;
    }

}
