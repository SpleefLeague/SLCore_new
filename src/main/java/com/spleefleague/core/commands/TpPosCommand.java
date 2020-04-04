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
import com.spleefleague.core.util.TpVector;
import java.util.List;
import org.bukkit.command.CommandSender;

/**
 * @author NickM13
 */
public class TpPosCommand extends CommandTemplate {
    
    public TpPosCommand() {
        super(TpPosCommand.class, "tppos", Rank.MODERATOR, Rank.BUILDER);
        setUsage("/tppos <player> <x> <y> <z>");
    }
    
    @CommandAnnotation
    public void tppos(CorePlayer sender, TpVector tpVector) {
        sender.teleport(tpVector);
    }
    @CommandAnnotation
    public void tpposPlayer(CommandSender sender, CorePlayer cp, TpVector tpVector) {
        cp.teleport(tpVector);
    }
    @CommandAnnotation
    public void tpposPlayers(CommandSender sender, List<CorePlayer> cps, TpVector tpVector) {
        for (CorePlayer cp : cps) {
            cp.teleport(tpVector);
        }
    }

}
