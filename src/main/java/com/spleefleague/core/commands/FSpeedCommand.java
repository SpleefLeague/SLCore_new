/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.commands;

import com.spleefleague.core.annotation.CommandAnnotation;
import com.spleefleague.core.command.CommandTemplate;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.player.Rank;

/**
 * @author NickM13
 */
public class FSpeedCommand extends CommandTemplate {
    
    public FSpeedCommand() {
        super(FSpeedCommand.class, "fspeed", Rank.MODERATOR, Rank.BUILDER);
        setUsage("/fspeed [player] <-10 to 10>");
        setDescription("Set flying speed");
    }
    
    @CommandAnnotation
    public void fspeed(CorePlayer sender, Double f) {
        f = Math.min(10, Math.max(-10, f)) / 10D;
        sender.getPlayer().setFlySpeed(f.floatValue());
        success(sender, "Fly speed set to " + f);
    }
    
    @CommandAnnotation
    public void fspeed(CorePlayer sender, CorePlayer cp, Double f) {
        f = Math.min(10, Math.max(-10, f)) / 10D;
        cp.getPlayer().setFlySpeed(f.floatValue());
        success(cp, "Fly speed set to " + f);
        success(sender, "Fly speed of " + cp.getDisplayName() + " set to " + f);
    }

}
