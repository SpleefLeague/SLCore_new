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
import org.bukkit.Bukkit;

/**
 * @author NickM13
 */
public class SudoCommand extends CommandTemplate {
    
    public SudoCommand() {
        super(SudoCommand.class, "sudo", Rank.DEVELOPER);
    }
    
    @CommandAnnotation
    public void sudo(CorePlayer sender, CorePlayer sudoer, String command) {
        Bukkit.dispatchCommand(sudoer.getPlayer(), command);
        success(sender, "Command run for " + sudoer.getDisplayName() + ": /" + command);
    }
    
}
