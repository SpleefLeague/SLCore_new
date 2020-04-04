/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.spleefleague.core.commands;

import com.spleefleague.core.Core;
import com.spleefleague.core.annotation.CommandAnnotation;
import com.spleefleague.core.command.CommandTemplate;
import com.spleefleague.core.player.CorePlayer;
import com.spleefleague.core.player.Rank;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/**
 * @author NickM13
 */
public class TempBanCommand extends CommandTemplate {
    
    public TempBanCommand() {
        super(TempBanCommand.class, "tempban", Rank.MODERATOR);
        setUsage("/tempban <player> <seconds> [reason]");
        setDescription("Temporarily ban a player from the server");
    }
    
    public long toMillis(String time) {
        long multiplier = 1000;
        switch (time.substring(time.length() - 1)) {
            case "y": multiplier *= 52;
            case "w": multiplier *= 7;
            case "d": multiplier *= 24;
            case "h": multiplier *= 60;
            case "m": multiplier *= 60;
                break;
        }
        time = time.substring(0, time.length() - 1);
        return Long.valueOf(time) * multiplier;
    }
    
    @CommandAnnotation
    public void tempban(CorePlayer sender, OfflinePlayer op, String time, String reason) {
        Core.getInstance().tempban(sender.getName(), op, toMillis(time), reason);
    }
    @CommandAnnotation
    public void tempban(CorePlayer sender, OfflinePlayer op, String time) {
        Core.getInstance().tempban(sender.getName(), op, toMillis(time), "");
    }
    @CommandAnnotation
    public void tempban(CommandSender sender, OfflinePlayer op, String time, String reason) {
        Core.getInstance().tempban(sender.getName(), op, toMillis(time), reason);
    }
    @CommandAnnotation
    public void tempban(CommandSender sender, OfflinePlayer op, String time) {
        Core.getInstance().tempban(sender.getName(), op, toMillis(time), "");
    }
    
}
