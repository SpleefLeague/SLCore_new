/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.commands;

import com.spleefleague.core.annotation.CommandAnnotation;
import com.spleefleague.core.command.CommandTemplate;
import com.spleefleague.core.error.CoreError;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.player.Rank;

/**
 * @author NickM13
 */
public class MuteCommand extends CommandTemplate {
    
    public MuteCommand() {
        super(MuteCommand.class, "mute", Rank.MODERATOR);
    }
    
    @CommandAnnotation
    public void mute(CorePlayer sender) {
        error(sender, CoreError.SETUP);
    }
    
}
